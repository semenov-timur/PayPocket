package com.paypocket.service;

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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    // Вспомогательные методы для создания тестовых данных
    // ============================================================

    private Wallet createWallet(String balance) {
        return createWalletWithId(UUID.randomUUID(), balance);
    }

    private Wallet createWalletWithId(UUID walletId, String balance) {
        return createWalletWithCurrency(walletId, balance, Currency.RUB);
    }

    private Wallet createWalletWithCurrency(UUID walletId, String balance, Currency currency) {
        Wallet wallet = new Wallet(userId, "Тестовый", currency);
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
