package com.paypocket.service;

import com.paypocket.config.DatabaseConnectionManager;
import com.paypocket.dto.TransferResult;
import com.paypocket.exception.*;
import com.paypocket.model.Currency;
import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;
import com.paypocket.model.Wallet;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.WalletRepository;
import com.paypocket.repository.jdbc.JdbcTransactionRepository;
import com.paypocket.repository.jdbc.JdbcWalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Сервис управления кошельками и денежными операциями.
 *
 * <p>Центральный сервис приложения. Отвечает за:</p>
 * <ul>
 *   <li>Создание кошельков</li>
 *   <li>Пополнение (deposit)</li>
 *   <li>Снятие (withdraw)</li>
 *   <li>Перевод между кошельками (transfer)</li>
 *   <li>Историю операций</li>
 * </ul>
 *
 * <p>Все денежные операции создают записи в {@link TransactionRepository}
 * для полной истории и аудита.</p>
 */
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final DatabaseConnectionManager connectionManager;

    private final Logger log = LoggerFactory.getLogger(WalletService.class);

    /**
     * Конструктор с внедрением зависимостей для Jdbc-режима.
     * С поддержкой БД-транзакций.
     *
     * @param walletRepository      репозиторий кошельков
     * @param transactionRepository репозиторий трензакций
     */
    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         DatabaseConnectionManager connectionManager) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.connectionManager =  connectionManager;
    }

    // ======================================
    // СОЗДАНИЕ КОШЕЛЬКА
    // ======================================

    /**
     * Создает новый кошелек для пользователя.
     *
     * @param userId    id владельца
     * @param name      название кошелька
     * @param currency  валюта кошелька (RUB, USD, EUR)
     * @return созданный кошелек с нулевым балансом
     */
    public Wallet createWallet(UUID userId, String name, Currency currency) {
        List<Wallet> existingWallets = walletRepository.findByUserId(userId);
       boolean currencyExists = existingWallets.stream()
               .anyMatch(wallet -> wallet.getCurrency() == currency);
       if (currencyExists && currency != Currency.RUB) {
           throw new WalletAlreadyExistsException(currency);
       }

       Wallet wallet = new Wallet(userId, name, currency);
       walletRepository.save(wallet);
       log.info("Wallet created: id – {}, userId – {}, name – {}, currency – {}", wallet.getId(), userId, name, currency);
       return wallet;
    }

    /**
     * По умолчанию создает рублевый кошелек.
     * @param userId    id владельца
     * @param name      название кошелька
     * @return созданный кошелек с нулевым балансом
     */
    public Wallet createWallet(UUID userId, String name) {
        Wallet wallet = new Wallet(userId, name, Currency.RUB);
        return walletRepository.save(wallet);
    }

    // ======================================
    // ПОПОЛНЕНИЕ
    // ======================================

    /**
     * Пополняет кошелек на указанную сумму.
     *
     * <p>Операция создает запись транзакции типа DEPOSIT для истории.</p>
     *
     * @param walletId  id кошелька
     * @param amount    сумма пополнения (> 0)
     * @return кошелёк с обновленным балансом
     * @throws com.paypocket.exception.WalletNotFoundException если кошелек не найден
     * @throws InvalidAmountException если сумма <= 0
     */
    public Wallet deposit(UUID walletId, BigDecimal amount) {
        validateAmount(amount);

        Wallet wallet = getWalletOrThrow(walletId);
        wallet.deposit(amount);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction.Builder(walletId, TransactionType.DEPOSIT, amount)
                .decscription("Пополнение кошелька")
                .build();
        transactionRepository.save(transaction);

        log.info("Deposit: walletId – {}, amount – {}", walletId, amount);
        return wallet;
    }

    // ======================================
    // СНЯТИЕ
    // ======================================

    /**
     * Снимает с кошелька указанную сумму.
     *
     * <p>Операция создает запись транзакции типа WITHDRAW для истории.</p>
     *
     * @param walletId  id кошелька
     * @param amount    сумма для снятия
     * @return кошелек с обновленным балансов
     * @throws com.paypocket.exception.WalletNotFoundException если кошелек не найден
     * @throws InvalidAmountException если сумма <= 0
     */
    public Wallet withdraw(UUID walletId, BigDecimal amount) {
        validateAmount(amount);

        Wallet wallet = getWalletOrThrow(walletId);
        wallet.withdraw(amount);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction.Builder(walletId, TransactionType.WITHDRAW, amount)
                .decscription("Снятие средств")
                .build();
        transactionRepository.save(transaction);

        log.info("Withdraw: walletId - {}, amount – {}", walletId, amount);
        return  wallet;
    }

    // ======================================
    // ПЕРЕВОД
    // ======================================

    /**
     * Переводит средства между кошельками.
     *
     * <p>Атомарная операция: либо выполняется полностью, либо не выполняется вообще.
     * Сейчас атомарность обеспечивается тем, что всё происходит в одном потоке
     * в памяти. Далее подключим БД-транзакцию, потом Spring @Transactional.</p>
     *
     * <p>Создаёт ДВЕ записи транзакций (двойная запись):
     * TRANSFER_OUT у отправителя, TRANSFER_IN у получателя.</p>
     *
     * @param fromWalletId  id кошелька отправителя
     * @param toWalletId    id кошелька получателя
     * @param amount        сумма перевода
     * @return результат перевода с полученными балансами
     * @throws InvalidAmountException                               если один из кошельков не найден
     * @throws SelfTransferException                                если отправитель и получатель совпадают
     * @throws com.paypocket.exception.WalletNotFoundException      если сумма перевода <= 0
     * @throws com.paypocket.exception.InsufficientFundsException   если в кошельке недостаточно средств для перевода
     */
    public TransferResult transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        validateAmount(amount);

        Connection connection = connectionManager.getConnection();

        try {
            connection.setAutoCommit(false);

            UUID firstId = fromWalletId.compareTo(toWalletId) < 0 ? fromWalletId : toWalletId;
            UUID secondId = fromWalletId.compareTo(toWalletId) < 0 ? toWalletId : fromWalletId;

            walletRepository.findByIdForUpdate(connection, firstId);
            walletRepository.findByIdForUpdate(connection, secondId);

            Wallet sender = walletRepository.findByIdForUpdate(connection, fromWalletId)
                    .orElseThrow(() -> new WalletNotFoundException(fromWalletId));
            Wallet receiver = walletRepository.findByIdForUpdate(connection, toWalletId)
                    .orElseThrow(() -> new WalletNotFoundException(toWalletId));

            if (!sender.getCurrency().equals(receiver.getCurrency())) {
                throw new CurrencyMismatchException(sender.getCurrency(), receiver.getCurrency());
            }

            if (sender.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(sender.getBalance(), amount);
            }

            BigDecimal senderNewBalance = sender.getBalance().subtract(amount);
            BigDecimal receiverNewBalance = receiver.getBalance().add(amount);

            walletRepository.updateBalance(connection, fromWalletId, senderNewBalance);
            walletRepository.updateBalance(connection, toWalletId, receiverNewBalance);

            Transaction out = new Transaction.Builder(fromWalletId, TransactionType.TRANSACTION_OUT, amount)
                    .counterpartyWalletId(toWalletId)
                    .decscription("Перевод отправлен")
                    .build();
            transactionRepository.save(connection, out);

            Transaction in = new Transaction.Builder(toWalletId, TransactionType.TRANSACTION_IN, amount)
                    .counterpartyWalletId(fromWalletId)
                    .decscription("Перевод получен")
                    .build();
            transactionRepository.save(connection, in);

            connection.commit();

            log.info("Transfer: {} → {}, amount={}, senderBalance={}, receiverBalance={}",
                    fromWalletId, toWalletId, amount, senderNewBalance, receiverNewBalance);

            return new TransferResult(
                    fromWalletId,
                    toWalletId,
                    amount,
                    senderNewBalance,
                    receiverNewBalance);

        } catch (SQLException e) {
            safeRollback(connection);
            log.error("Transfer DB error: {} → {}, amount={}", fromWalletId, toWalletId, amount, e);
            throw new RuntimeException("Ошибка БД при переводе: " + e.getMessage(), e);
        } catch (PayPocketException e) {
            safeRollback(connection);
            log.warn("Transfer rejected: {} → {}, amount={}, reason={}",fromWalletId, toWalletId, amount, e.getMessage());
            throw e;
        } finally {
            safeClose(connection);
        }

    }

    // ================================================================
    // ЗАПРОСЫ
    // ================================================================

    /**
     * Возвращает кошелек по id.
     *
     * @param walletId id кошелька
     * @return кошелек
     * @throws com.paypocket.exception.WalletNotFoundException если кошелек не найден
     */
    public Wallet getWallet(UUID walletId) {
        return this.getWalletOrThrow(walletId);
    }

    /**
     * Возвращает все кошельки пользователя.
     *
     * @param userId id пользователя
     * @return список найденных кошельков
     */
    public List<Wallet> getUserWallets(UUID userId) {
        return this.walletRepository.findByUserId(userId);
    }

    /**
     * Возвращает баланс кошелька.
     *
     * @param walletId id кошелька
     * @return баланс кошелька
     * @throws com.paypocket.exception.WalletNotFoundException если кошелек не найден
     */
    public BigDecimal getBalance(UUID walletId) {
        return this.getWalletOrThrow(walletId).getBalance();
    }

    /**
     * Возвращает историю всех операций кошелька.
     *
     * @param walletId id кошелька
     * @return список операций
     * @throws WalletNotFoundException если кошелек не найден
     */
    public List<Transaction> getTransactionHistory(UUID walletId) {
        // проверяем, что кошелек существует
        getWalletOrThrow(walletId);
        return this.transactionRepository.findByWalletId(walletId);
    }

    /**
     * Возвращает историю операций кошелька определенного типа.
     *
     * @param walletId id кошелька
     * @return список операций
     * @throws WalletNotFoundException если кошелек не найден
     */
    public List<Transaction> getTransactionHistory(UUID walletId, TransactionType type) {
        // проверяем, что кошелек существует
        getWalletOrThrow(walletId);
        return this.transactionRepository.findByWalletIdAndType(walletId, type);
    }

    // ================================================================
    // ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ================================================================

    /**
     * Находит кошелек или бросает исключение.
     */
    private Wallet getWalletOrThrow(UUID walletId) {
        return  this.walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null ||  amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2) {
            throw new InvalidAmountException(amount);
        }
    }

    private void safeRollback(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при rollback: " + e.getMessage());
        }
    }

    private void safeClose(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.setAutoCommit(true);
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }
}
