package com.paypocket.service;

import com.paypocket.dto.ConversionResult;
import com.paypocket.dto.TransferResult;
import com.paypocket.exception.*;
import com.paypocket.model.Currency;
import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;
import com.paypocket.model.Wallet;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;

    private final Logger log = LoggerFactory.getLogger(WalletService.class);

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param walletRepository      репозиторий кошельков
     * @param transactionRepository репозиторий трензакций
     * @param exchangeRateService   сервис курсов валют
     */
    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         ExchangeRateService exchangeRateService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeRateService = exchangeRateService;
    }

    // ======================================
    // СОЗДАНИЕ КОШЕЛЬКА
    // ======================================

    /**
     * Создает новый кошелек для пользователя.
     *
     * @param userId   id владельца
     * @param name     название кошелька
     * @param currency валюта кошелька (RUB, USD, EUR)
     * @return созданный кошелек с нулевым балансом
     */
    @Transactional
    public Wallet createWallet(UUID userId, String name, Currency currency) {
        if (currency != Currency.RUB) {
            List<Wallet> existingWallets = walletRepository.findByUserIdOrderByCreatedAtAsc(userId);
            boolean currencyExists = existingWallets.stream()
                    .anyMatch(wallet -> wallet.getCurrency() == currency);
            if (currencyExists) {
                throw new WalletAlreadyExistsException(currency);
            }
        }

        Wallet wallet = new Wallet(userId, name, currency);
        walletRepository.save(wallet);

        log.info("Wallet created: id – {}, userId – {}, name – {}, currency – {}",
                wallet.getId(), userId, name, currency);
        return wallet;
    }

    /**
     * По умолчанию создает рублевый кошелек.
     *
     * @param userId id владельца
     * @param name   название кошелька
     * @return созданный кошелек с нулевым балансом
     */
    @Transactional
    public Wallet createWallet(UUID userId, String name) {
        return createWallet(userId, name, Currency.RUB);
    }

    // ======================================
    // ПОПОЛНЕНИЕ
    // ======================================

    /**
     * Пополняет кошелек на указанную сумму.
     *
     * <p>Операция создает запись транзакции типа DEPOSIT для истории.</p>
     *
     * @param walletId id кошелька
     * @param amount   сумма пополнения (> 0)
     * @return кошелёк с обновленным балансом
     * @throws com.paypocket.exception.WalletNotFoundException если кошелек не найден
     * @throws InvalidAmountException                          если сумма <= 0
     */
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {
        validateAmount(amount);

        Wallet wallet = getWalletOrThrow(walletId);
        wallet.deposit(amount);

        Transaction transaction = new Transaction.Builder(walletId, TransactionType.DEPOSIT, amount)
                .decscription("Пополнение кошелька")
                .build();
        transactionRepository.save(transaction);

        log.info("Deposit: walletId – {}, amount – {}, newBalance – {}",
                walletId, amount, wallet.getBalance());
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
     * @param walletId id кошелька
     * @param amount   сумма для снятия
     * @return кошелек с обновленным балансов
     * @throws com.paypocket.exception.WalletNotFoundException если кошелек не найден
     * @throws InvalidAmountException                          если сумма <= 0
     */
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {
        validateAmount(amount);

        Wallet wallet = getWalletOrThrow(walletId);
        wallet.withdraw(amount);

        Transaction transaction = new Transaction.Builder(walletId, TransactionType.WITHDRAW, amount)
                .decscription("Снятие средств")
                .build();
        transactionRepository.save(transaction);

        log.info("Withdraw: walletId - {}, amount – {}, newBalance – {}",
                walletId, amount, wallet.getBalance());
        return wallet;
    }

    // ======================================
    // ПЕРЕВОД
    // ======================================

    /**
     * Переводит средства между кошельками.
     *
     * <p>Spring @Transactional делает все автоматически:
     * <ul>
     *      <li>Открывает транзакцию перед методом</li>
     *      <li>COMMIT при успешном завершении</li>
     *      <li>ROLLBACK при любом исключении</li>
     * </ul>.
     * </p>
     *
     * <p>findByIdForUpdate() добавляет SELECT FOR UPDATE —
     * блокировка строк, как в JDBC-версии.</p>
     *
     * <p>Создаёт ДВЕ записи транзакций (двойная запись):
     * TRANSFER_OUT у отправителя, TRANSFER_IN у получателя.</p>
     *
     * @param fromWalletId id кошелька отправителя
     * @param toWalletId   id кошелька получателя
     * @param amount       сумма перевода
     * @return результат перевода с полученными балансами
     * @throws InvalidAmountException                             если один из кошельков не найден
     * @throws SelfTransferException                              если отправитель и получатель совпадают
     * @throws com.paypocket.exception.WalletNotFoundException    если сумма перевода <= 0
     * @throws com.paypocket.exception.InsufficientFundsException если в кошельке недостаточно средств для перевода
     */
    @Transactional
    public TransferResult transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        validateAmount(amount);

        if (fromWalletId.equals(toWalletId)) {
            throw new SelfTransferException(fromWalletId);
        }

        UUID firstId = fromWalletId.compareTo(toWalletId) < 0 ? fromWalletId : toWalletId;
        UUID secondId = fromWalletId.compareTo(toWalletId) < 0 ? toWalletId : fromWalletId;

        walletRepository.findByIdForUpdate(firstId);
        walletRepository.findByIdForUpdate(secondId);

        Wallet sender = walletRepository.findByIdForUpdate(fromWalletId)
                .orElseThrow(() -> new WalletNotFoundException(fromWalletId));
        Wallet receiver = walletRepository.findByIdForUpdate(toWalletId)
                .orElseThrow(() -> new WalletNotFoundException(toWalletId));

        if (sender.getCurrency() != receiver.getCurrency()) {
            throw new CurrencyMismatchException(sender.getCurrency(), receiver.getCurrency());
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), amount);
        }

        sender.withdraw(amount);
        receiver.deposit(amount);

        Transaction out = new Transaction.Builder(fromWalletId, TransactionType.TRANSACTION_OUT, amount)
                .counterpartyWalletId(toWalletId)
                .decscription("Перевод отправлен")
                .build();
        transactionRepository.save(out);

        Transaction in = new Transaction.Builder(toWalletId, TransactionType.TRANSACTION_IN, amount)
                .counterpartyWalletId(fromWalletId)
                .decscription("Перевод получен")
                .build();
        transactionRepository.save(in);


        log.info("Transfer: {} → {}, amount={}, senderBalance={}, receiverBalance={}",
                fromWalletId, toWalletId, amount, sender.getBalance(), receiver.getBalance());

        return new TransferResult(
                fromWalletId,
                toWalletId,
                amount,
                sender.getBalance(),
                receiver.getBalance()
        );
    }

    // ======================================
    // КОНВЕРТАЦИЯ ВАЛЮТ
    // ======================================

    /**
     * Конвертирует средства между двумя кошельками одного пользователя
     * с разными валютами.
     *
     * <p>Списывает {@code amount} в валюте кошелька-источника, рассчитывает
     * эквивалент по текущему курсу из {@link ExchangeRateService}
     * и зачисляет получившуюся сумму на кошелёк-получатель.</p>
     *
     * <p>Создаёт две записи в истории: {@link TransactionType#CONVERT_OUT}
     * у источника и {@link TransactionType#CONVERT_IN} у получателя.</p>
     *
     * @param fromWalletId id кошелька-источника
     * @param toWalletId   id кошелька-получателя
     * @param amount       сумма списания в валюте источника
     * @return результат конвертации с применённым курсом и итоговыми балансами
     * @throws InvalidAmountException     если сумма <= 0
     * @throws SelfTransferException      если указан один и тот же кошелёк
     * @throws WalletNotFoundException    если один из кошельков не найден
     * @throws WalletOwnershipException   если кошельки принадлежат разным пользователям
     * @throws CurrencyMismatchException  если валюты кошельков совпадают (нужно использовать transfer)
     * @throws InsufficientFundsException если средств на кошельке-источнике недостаточно
     */
    @Transactional
    public ConversionResult convert(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        validateAmount(amount);

        if (fromWalletId.equals(toWalletId)) {
            throw new SelfTransferException(fromWalletId);
        }

        UUID firstId = fromWalletId.compareTo(toWalletId) < 0 ? fromWalletId : toWalletId;
        UUID secondId = fromWalletId.compareTo(toWalletId) < 0 ? toWalletId : fromWalletId;

        walletRepository.findByIdForUpdate(firstId);
        walletRepository.findByIdForUpdate(secondId);

        Wallet source = walletRepository.findByIdForUpdate(fromWalletId)
                .orElseThrow(() -> new WalletNotFoundException(fromWalletId));
        Wallet target = walletRepository.findByIdForUpdate(toWalletId)
                .orElseThrow(() -> new WalletNotFoundException(toWalletId));

        if (!source.getUserId().equals(target.getUserId())) {
            throw new WalletOwnershipException(fromWalletId, toWalletId);
        }

        if (source.getCurrency() == target.getCurrency()) {
            throw new CurrencyMismatchException(source.getCurrency(), target.getCurrency());
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(source.getBalance(), amount);
        }

        BigDecimal rate = exchangeRateService.getRate(source.getCurrency(), target.getCurrency());
        BigDecimal convertedAmount = exchangeRateService.convert(amount, source.getCurrency(), target.getCurrency());

        if (convertedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(convertedAmount);
        }

        source.withdraw(amount);
        target.deposit(convertedAmount);

        Transaction out = new Transaction.Builder(fromWalletId, TransactionType.CONVERT_OUT, amount)
                .counterpartyWalletId(toWalletId)
                .decscription(String.format("Конвертация в %s", target.getCurrency()))
                .build();
        transactionRepository.save(out);

        Transaction in = new Transaction.Builder(toWalletId, TransactionType.CONVERT_IN, convertedAmount)
                .counterpartyWalletId(fromWalletId)
                .decscription(String.format("Конвертация из %s", source.getCurrency()))
                .build();
        transactionRepository.save(in);

        log.info("Convert: {} {} → {} {}, rate={}, sourceBalance={}, targetBalance={}",
                amount, source.getCurrency(),
                convertedAmount, target.getCurrency(),
                rate, source.getBalance(), target.getBalance());

        return new ConversionResult(
                fromWalletId,
                toWalletId,
                source.getCurrency(),
                target.getCurrency(),
                amount,
                convertedAmount,
                rate,
                source.getBalance(),
                target.getBalance()
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
        return this.walletRepository.findByUserIdOrderByCreatedAtAsc(userId);
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
        return this.transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }

    /**
     * Возвращает историю операций кошелька с пагинацией.
     *
     * @param walletId id кошелька
     * @return список операций
     * @throws WalletNotFoundException если кошелек не найден
     */
    public Page<Transaction> getTransactionHistory(UUID walletId, int pageNumber, int pageSize) {
        // проверяем, что кошелек существует
        getWalletOrThrow(walletId);
        return this.transactionRepository.findByWalletId(
                walletId,
                PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending())
        );
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
        return this.transactionRepository.findByWalletIdAndTypeOrderByCreatedAtDesc(walletId, type);
    }

    // ================================================================
    // ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ================================================================

    /**
     * Находит кошелек или бросает исключение.
     */
    private Wallet getWalletOrThrow(UUID walletId) {
        return this.walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2) {
            throw new InvalidAmountException(amount);
        }
    }
}
