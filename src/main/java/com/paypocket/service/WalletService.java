package com.paypocket.service;


import com.paypocket.dto.TransferResult;
import com.paypocket.exception.*;
import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;
import com.paypocket.model.Wallet;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.WalletRepository;

import java.math.BigDecimal;
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

    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;

    /**
     * @param walletRepository      репозиторий кошельков
     * @param transactionRepository репозиторий трензакций
     */
    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
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
    public Wallet createWallet(UUID userId, String name, String currency) {
        List<Wallet> existingWallets = walletRepository.findByUserId(userId);
       boolean currencyExists = existingWallets.stream()
               .anyMatch(wallet -> wallet.getCurrency().equalsIgnoreCase(currency));
       if (currencyExists) {
           throw new WalletAlreadyExistsException(userId, currency);
       }

        Wallet wallet = new Wallet(userId, name, currency);
        return walletRepository.save(wallet);
    }

    /**
     * По умолчанию создает рублевый кошелек.
     * @param userId    id владельца
     * @param name      название кошелька
     * @return созданный кошелек с нулевым балансом
     */
    public Wallet createWallet(UUID userId, String name) {
        Wallet wallet = new Wallet(userId, name, "RUB");
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

        Transaction transaction = new Transaction.Builder(walletId, TransactionType.DEPOSIT, amount)
                .decscription("Пополнение кошелька")
                .build();
        transactionRepository.save(transaction);

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

        Transaction transaction = new Transaction.Builder(walletId, TransactionType.WITHDRAW, amount)
                .decscription("Снятие средств")
                .build();
        transactionRepository.save(transaction);

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

        if (fromWalletId.equals(toWalletId)) {
            throw new SelfTransferException(toWalletId);
        }

        Wallet senderWallet = getWalletOrThrow(fromWalletId);
        Wallet receiverWallet = getWalletOrThrow(toWalletId);

        if (!senderWallet.getCurrency().equalsIgnoreCase(receiverWallet.getCurrency())) {
            throw new CurrencyMismatchException(senderWallet.getCurrency(), receiverWallet.getCurrency());
        }

        senderWallet.withdraw(amount);

        Transaction out = new Transaction.Builder(fromWalletId, TransactionType.TRANSACTION_OUT, amount)
                .counterpartyWalletId(toWalletId)
                .decscription("Перевод отправлен")
                .build();
        transactionRepository.save(out);

        try {
            receiverWallet.deposit(amount);
        } catch (PayPocketException e) {
            senderWallet.deposit(amount);

            Transaction in = new Transaction.Builder(fromWalletId, TransactionType.TRANSACTION_IN, amount)
                    .decscription("Перевод не выполнен, средства возвращены.")
                    .build();
            transactionRepository.save(in);
            throw new PayPocketException("Перевод не выполнен, средства возвращены", e);
        }

        Transaction in = new Transaction.Builder(toWalletId, TransactionType.TRANSACTION_IN, amount)
                .counterpartyWalletId(fromWalletId)
                .decscription("Перевод получен")
                .build();
        transactionRepository.save(in);

        return new TransferResult(
                fromWalletId,
                toWalletId,
                amount,
                senderWallet.getBalance(),
                receiverWallet.getBalance()
        );
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
        if (amount == null ||  amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(amount);
        }
    }
}
