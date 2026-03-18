package com.paypocket;

import com.paypocket.dto.TransferResult;
import com.paypocket.exception.*;
import com.paypocket.model.*;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;
import com.paypocket.repository.inmemory.InMemoryTransactionRepository;
import com.paypocket.repository.inmemory.InMemoryUserRepository;
import com.paypocket.repository.inmemory.InMemoryWalletRepository;
import com.paypocket.service.UserService;
import com.paypocket.service.WalletService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Точка входа в приложние PayPocket.
 */
public class PayPocketApp {
    public static void main(String[] args) {
        System.out.println("===== PayPocket – Digital Wallet =====\n");

        UserRepository userRepository = new InMemoryUserRepository();
        WalletRepository walletRepository = new InMemoryWalletRepository();
        TransactionRepository transactionRepository = new InMemoryTransactionRepository();

        UserService userService = new UserService(userRepository);
        WalletService walletService = new WalletService(walletRepository, transactionRepository);

        // =====================================
        // Сценарий 1: Регистрация пользователей
        // =====================================
        System.out.println("––––– Регистрация пользователей –––––");

        User mia = userService.register("mia", "miyaskam@icloud.com", "pass1234");
        System.out.println("Зарегестрирован: " + mia.getUsername());

        User timur = userService.register("timur", "semenov-timur@icloud.com", "pass1234");
        System.out.println("Зарегестрирован: " + timur.getUsername());

        try {
            userService.register("timur", "semenov-timur@icloud.com", "pass1234");
        } catch (DuplicateUserException e) {
            System.out.println("Ожидаемая ошибка: " + e.getMessage());
        }

        // =====================================
        // Сценарий 2: Создание кошельков
        // =====================================
        System.out.println("––––– Создание кошельков –––––");

        Wallet miaWallet = walletService.createWallet(mia.getId(), "Основной");
        System.out.println("Кошелек Мии: " + miaWallet.getName() + " (" + miaWallet.getBalance() + " " + miaWallet.getCurrency() + ")");

        Wallet timurWallet = walletService.createWallet(timur.getId(), "Основной");
        System.out.println("Кошелек Тимура: " + timurWallet.getName() + " (" + timurWallet.getBalance() + " " + timurWallet.getCurrency() + ")");

        Wallet timurUsdWallet = walletService.createWallet(timur.getId(), "Долларовый", "USD");
        System.out.println("Второй кошелек Тимура: " + timurUsdWallet.getName() + " (" + timurUsdWallet.getBalance() + " " + timurUsdWallet.getCurrency() + ")");

        // =====================================
        // Сценарий 3: Пополнение
        // =====================================
        System.out.println("––––– Пополнение –––––");

        walletService.deposit(miaWallet.getId(), new BigDecimal("10000.00"));
        walletService.deposit(timurWallet.getId(), new BigDecimal("5000.00"));
        walletService.deposit(timurUsdWallet.getId(), new BigDecimal("500.00"));

        System.out.println("Баланс Мии (RUB): " + walletService.getBalance(miaWallet.getId()));
        System.out.println("Баланс Тимура (RUB): " + walletService.getBalance(timurWallet.getId()));
        System.out.println("Баланс Тимура (USD): " + walletService.getBalance(timurUsdWallet.getId()));

        // =====================================
        // Сценарий 4: Перевод
        // =====================================
        System.out.println("––––– Перевод –––––");

        TransferResult result = walletService.transfer(
                miaWallet.getId(),
                timurWallet.getId(),
                new BigDecimal("2500.00")
        );

        System.out.println("Перевод выполнен!");
        System.out.println("Баланс Мии: " + result.getSenderBalanceAfter() + " RUB");
        System.out.println("Баланс Тимура: " + result.getReceiverBalanceAfter() + " RUB");

        // =====================================
        // Сценарий 5: Обработка ошибок
        // =====================================
        System.out.println("––––– Обработка ошибок –––––");

        // Самому себе
        try {
            walletService.transfer(
                    miaWallet.getId(),
                    miaWallet.getId(),
                    new BigDecimal("2500.00")
            );
        } catch (SelfTransferException e) {
            System.out.println("[OK] - " + e.getMessage());
        }

        // Недостаточно средств
        try {
            walletService.transfer(
                    miaWallet.getId(),
                    timurWallet.getId(),
                    new BigDecimal("9999.00")
            );
        } catch (InsufficientFundsException e) {
            System.out.println("[OK] - " + e.getMessage());
        }

        // Некорректная сумма
        try {
            walletService.withdraw(miaWallet.getId(), new BigDecimal("-100.00"));
        } catch (InvalidAmountException e) {
            System.out.println("[OK] - " + e.getMessage());
        }

        // Несуществующий кошелек
        try {
            walletService.getBalance(UUID.randomUUID());
        } catch (WalletNotFoundException e) {
            System.out.println("[OK] - " + e.getMessage());
        }

        // =====================================
        // Сценарий 6: История операций
        // =====================================
        System.out.println("––––– История операций Мии –––––");

        List<Transaction> miaHistory = walletService.getTransactionHistory(miaWallet.getId());
        for (Transaction transaction : miaHistory) {
            System.out.printf(" %-14s %10s RUB %s%n",
                    transaction.getType(),
                    transaction.getAmount(),
                    transaction.getDescription());
        }

        System.out.println("––––– История операций Тимура –––––");

        List<Transaction> timurHistory = walletService.getTransactionHistory(timurWallet.getId());
        for (Transaction transaction : timurHistory) {
            System.out.printf(" %-14s %10s RUB %s%n",
                    transaction.getType(),
                    transaction.getAmount(),
                    transaction.getDescription());
        }

        List<Transaction> timurIncoming = walletService.getTransactionHistory(timurWallet.getId(), TransactionType.TRANSACTION_IN);
        System.out.println("Входящих переводов Тимура: " +  timurIncoming.size());

        // =====================================
        // Сценарий 7: Все кошельки пользователя
        // =====================================
        System.out.println("––––– Все кошельки Тимура –––––");

        List<Wallet> timurWallets = walletService.getUserWallets(timur.getId());
        for (Wallet wallet : timurWallets) {
            System.out.printf(" %s: %s %s%n",
                    wallet.getName(),
                    wallet.getBalance(),
                    wallet.getCurrency());
        }

        // =====================================
        // Итог
        // =====================================
        System.out.println("––––– Все сценарии пройдены успешно! –––––");
    }
}
