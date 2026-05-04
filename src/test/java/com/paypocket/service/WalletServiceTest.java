package com.paypocket.service;

import com.paypocket.dto.ConversionResult;
import com.paypocket.dto.TransferResult;
import com.paypocket.exception.*;
import com.paypocket.model.Currency;
import com.paypocket.model.Transaction;
import com.paypocket.model.Wallet;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService – юнит-тесты")
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private WalletService walletService;

    private final UUID userId = UUID.randomUUID();

    // ============================================================
    // DEPOSIT
    // ============================================================

    @Test
    @DisplayName("Пополнение: баланс увеличивается на сумму депозита")
    void deposit_shouldIncreaseBalance() {
        Wallet wallet = createWallet("1000.00");

        when(walletRepository.findById(wallet.getId()))
                .thenReturn(Optional.of(wallet));

        walletService.deposit(wallet.getId(), new BigDecimal("500.00"));

        assertEquals(new BigDecimal("1500.00"), wallet.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Пополнение: отрицательная сумма — InvalidAmountException")
    void deposit_negativeAmount_shouldThrow() {
        assertThrows(InvalidAmountException.class, () ->
                walletService.deposit(UUID.randomUUID(), new BigDecimal("-100.00")));

        verify(walletRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Пополнение: нулевая сумма — InvalidAmountException")
    void deposit_zeroAmount_shouldThrow() {
        assertThrows(InvalidAmountException.class, () ->
                walletService.deposit(UUID.randomUUID(), BigDecimal.ZERO));

        verify(walletRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Пополнение: несуществующий кошелёк — WalletNotFoundException")
    void deposit_walletNotFound_shouldThrow() {
        UUID fakeId =  UUID.randomUUID();
        when(walletRepository.findById(fakeId)).
                thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                walletService.deposit(fakeId, new BigDecimal("1000.00")));
    }

    // ============================================================
    // TRANSFER
    // ============================================================

    @Test
    @DisplayName("Перевод: успешный перевод между кошельками")
    void transfer_shouldMoveFunds() {
        Wallet sender = createWallet("5000.00");
        Wallet receiver = createWallet("1000.00");

        when(walletRepository.findByIdForUpdate(sender.getId()))
                .thenReturn(Optional.of(sender));
        when(walletRepository.findByIdForUpdate(receiver.getId()))
                .thenReturn(Optional.of(receiver));

        TransferResult result = walletService.transfer(
                sender.getId(), receiver.getId(), new BigDecimal("500.00"));

        assertEquals(new BigDecimal("4500.00"), result.getSenderBalanceAfter());
        assertEquals(new BigDecimal("1500.00"), result.getReceiverBalanceAfter());
        assertEquals(new BigDecimal("4500.00"), sender.getBalance());
        assertEquals(new BigDecimal("1500.00"), receiver.getBalance());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Перевод: недостаточно средств — InsufficientFundsException")
    void transfer_insufficientFunds_shouldThrow() {
        Wallet sender = createWallet("10.00");
        Wallet receiver = createWallet("0.00");

        when(walletRepository.findByIdForUpdate(sender.getId()))
                .thenReturn(Optional.of(sender));
        when(walletRepository.findByIdForUpdate(receiver.getId()))
                .thenReturn(Optional.of(receiver));

        assertThrows(InsufficientFundsException.class, () ->
                walletService.transfer(sender.getId(), receiver.getId(), new BigDecimal("50.00")));

        assertEquals(new BigDecimal("10.00"), sender.getBalance());
        assertEquals(BigDecimal.ZERO, receiver.getBalance());

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод самому себе — SelfTransferException")
    void transfer_selfTransfer_shouldThrow() {
        UUID walletId = UUID.randomUUID();

        assertThrows(SelfTransferException.class, () ->
                walletService.transfer(walletId, walletId, BigDecimal.TEN));

        verify(walletRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Перевод: разные валюты — CurrencyMismatchException")
    void transfer_currencyMismatch_shouldThrow() {
        Wallet rubWallet = createWalletWithCurrency(UUID.randomUUID(), "100.00", Currency.RUB);
        Wallet usdWallet = createWalletWithCurrency(UUID.randomUUID(), "50.00", Currency.USD);

        when(walletRepository.findByIdForUpdate(rubWallet.getId()))
                .thenReturn(Optional.of(rubWallet));
        when(walletRepository.findByIdForUpdate(usdWallet.getId()))
                .thenReturn(Optional.of(usdWallet));

        assertThrows(CurrencyMismatchException.class, () ->
                walletService.transfer(rubWallet.getId(), usdWallet.getId(), new BigDecimal("50.00")));

        verify(transactionRepository, never()).save(any());
    }

    // ============================================================
    // CONVERT
    // ============================================================

    @Test
    @DisplayName("Конвертация: успешная между RUB и USD одного пользователя")
    void convert_shouldMoveFundsAcrossCurrencies() {
        Wallet rubWallet = createWalletWithCurrency(UUID.randomUUID(), "9000.00", Currency.RUB);
        Wallet usdWallet = createWalletWithCurrency(UUID.randomUUID(), "0.00", Currency.USD);

        when(walletRepository.findByIdForUpdate(rubWallet.getId()))
                .thenReturn(Optional.of(rubWallet));
        when(walletRepository.findByIdForUpdate(usdWallet.getId()))
                .thenReturn(Optional.of(usdWallet));

        BigDecimal rate = BigDecimal.ONE.divide(new BigDecimal("90"), 8, RoundingMode.HALF_UP);
        BigDecimal converted = new BigDecimal("900.00").multiply(rate).setScale(2, RoundingMode.HALF_UP);

        when(exchangeRateService.getRate(Currency.RUB, Currency.USD)).thenReturn(rate);
        when(exchangeRateService.convert(new BigDecimal("900.00"), Currency.RUB, Currency.USD))
                .thenReturn(converted);

        ConversionResult result = walletService.convert(
                rubWallet.getId(), usdWallet.getId(), new BigDecimal("900.00"));

        assertEquals(new BigDecimal("8100.00"), rubWallet.getBalance());
        assertEquals(converted, usdWallet.getBalance());
        assertEquals(Currency.RUB, result.getFromCurrency());
        assertEquals(Currency.USD, result.getToCurrency());
        assertEquals(new BigDecimal("900.00"), result.getFromAmount());
        assertEquals(converted, result.getToAmount());
        assertEquals(rate, result.getRate());
        assertEquals(new BigDecimal("8100.00"), result.getFromBalanceAfter());
        assertEquals(converted, result.getToBalanceAfter());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Конвертация: отрицательная сумма — InvalidAmountException")
    void convert_negativeAmount_shouldThrow() {
        assertThrows(InvalidAmountException.class, () ->
                walletService.convert(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("-1.00")));

        verify(walletRepository, never()).findByIdForUpdate(any());
        verify(exchangeRateService, never()).convert(any(), any(), any());
    }

    @Test
    @DisplayName("Конвертация: один и тот же кошелёк — SelfTransferException")
    void convert_selfWallet_shouldThrow() {
        UUID walletId = UUID.randomUUID();

        assertThrows(SelfTransferException.class, () ->
                walletService.convert(walletId, walletId, new BigDecimal("10.00")));

        verify(walletRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Конвертация: кошелёк-источник не найден — WalletNotFoundException")
    void convert_sourceNotFound_shouldThrow() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        when(walletRepository.findByIdForUpdate(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                walletService.convert(fromId, toId, new BigDecimal("100.00")));

        verify(transactionRepository, never()).save(any());
        verify(exchangeRateService, never()).convert(any(), any(), any());
    }

    @Test
    @DisplayName("Конвертация: кошельки разных пользователей — WalletOwnershipException")
    void convert_differentOwners_shouldThrow() {
        UUID otherUserId = UUID.randomUUID();
        Wallet source = createWalletWithCurrency(UUID.randomUUID(), "1000.00", Currency.RUB);
        Wallet target = createWalletWithOwnerAndCurrency(
                UUID.randomUUID(), otherUserId, "0.00", Currency.USD);

        when(walletRepository.findByIdForUpdate(source.getId()))
                .thenReturn(Optional.of(source));
        when(walletRepository.findByIdForUpdate(target.getId()))
                .thenReturn(Optional.of(target));

        assertThrows(WalletOwnershipException.class, () ->
                walletService.convert(source.getId(), target.getId(), new BigDecimal("100.00")));

        assertEquals(new BigDecimal("1000.00"), source.getBalance());
        verify(transactionRepository, never()).save(any());
        verify(exchangeRateService, never()).convert(any(), any(), any());
    }

    @Test
    @DisplayName("Конвертация: совпадающие валюты — CurrencyMismatchException")
    void convert_sameCurrency_shouldThrow() {
        Wallet source = createWalletWithCurrency(UUID.randomUUID(), "1000.00", Currency.RUB);
        Wallet target = createWalletWithCurrency(UUID.randomUUID(), "0.00", Currency.RUB);

        when(walletRepository.findByIdForUpdate(source.getId()))
                .thenReturn(Optional.of(source));
        when(walletRepository.findByIdForUpdate(target.getId()))
                .thenReturn(Optional.of(target));

        assertThrows(CurrencyMismatchException.class, () ->
                walletService.convert(source.getId(), target.getId(), new BigDecimal("100.00")));

        verify(transactionRepository, never()).save(any());
        verify(exchangeRateService, never()).convert(any(), any(), any());
    }

    @Test
    @DisplayName("Конвертация: недостаточно средств — InsufficientFundsException")
    void convert_insufficientFunds_shouldThrow() {
        Wallet source = createWalletWithCurrency(UUID.randomUUID(), "10.00", Currency.RUB);
        Wallet target = createWalletWithCurrency(UUID.randomUUID(), "0.00", Currency.USD);

        when(walletRepository.findByIdForUpdate(source.getId()))
                .thenReturn(Optional.of(source));
        when(walletRepository.findByIdForUpdate(target.getId()))
                .thenReturn(Optional.of(target));

        assertThrows(InsufficientFundsException.class, () ->
                walletService.convert(source.getId(), target.getId(), new BigDecimal("500.00")));

        assertEquals(new BigDecimal("10.00"), source.getBalance());
        assertEquals(BigDecimal.ZERO, target.getBalance());
        verify(transactionRepository, never()).save(any());
        verify(exchangeRateService, never()).convert(any(), any(), any());
    }

    @Test
    @DisplayName("Конвертация: курс округляется до нуля — InvalidAmountException")
    void convert_zeroConverted_shouldThrow() {
        Wallet source = createWalletWithCurrency(UUID.randomUUID(), "100.00", Currency.RUB);
        Wallet target = createWalletWithCurrency(UUID.randomUUID(), "0.00", Currency.USD);

        when(walletRepository.findByIdForUpdate(source.getId()))
                .thenReturn(Optional.of(source));
        when(walletRepository.findByIdForUpdate(target.getId()))
                .thenReturn(Optional.of(target));

        when(exchangeRateService.getRate(Currency.RUB, Currency.USD))
                .thenReturn(new BigDecimal("0.00000001"));
        when(exchangeRateService.convert(eq(new BigDecimal("0.01")), eq(Currency.RUB), eq(Currency.USD)))
                .thenReturn(new BigDecimal("0.00"));

        assertThrows(InvalidAmountException.class, () ->
                walletService.convert(source.getId(), target.getId(), new BigDecimal("0.01")));

        assertEquals(new BigDecimal("100.00"), source.getBalance());
        assertEquals(BigDecimal.ZERO, target.getBalance());
        verify(transactionRepository, never()).save(any());
    }

    // ============================================================
    // Вспомогательные методы для создания тестовых данных
    // ============================================================

    private Wallet createWallet(String balance) {
        return createWalletWithId(UUID.randomUUID(), balance);
    }

    private Wallet createWalletWithId(UUID walletId, String balance) {
        return createWalletWithCurrency(walletId, balance, Currency.RUB);
    }

    private Wallet createWalletWithCurrency(UUID walletId, String balance, Currency currency) {
        return createWalletWithOwnerAndCurrency(walletId, userId, balance, currency);
    }

    private Wallet createWalletWithOwnerAndCurrency(UUID walletId,
                                                    UUID ownerId,
                                                    String balance,
                                                    Currency currency) {
        Wallet wallet = new Wallet(ownerId, "Тестовый", currency);
        try {
            // Устанавливаем ID (и баланс, если нужно) через рефлексию
            var idField = Wallet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(wallet, walletId);

            if (new BigDecimal(balance).compareTo(BigDecimal.ZERO) > 0) {
                var balanceField = Wallet.class.getDeclaredField("balance");
                balanceField.setAccessible(true);
                balanceField.set(wallet, new BigDecimal(balance));
            }
        } catch (Exception e) {
            throw new  RuntimeException(e);
        }
        return wallet;
    }
}
