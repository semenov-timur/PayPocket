package com.paypocket;

import com.paypocket.model.*;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;
import com.paypocket.repository.inmemory.InMemoryTransactionRepository;
import com.paypocket.repository.inmemory.InMemoryUserRepository;
import com.paypocket.repository.inmemory.InMemoryWalletRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Точка входа в приложние PayPocket.
 */
public class PayPocketApp {
    public static void main(String[] args) {
        System.out.println("===== PayPocket – Digital Wallet =====\n");

        // Инициализация репозиторием
        // тип переменной – интерфейс, а не реализация
        // Пример реализации полиморфизма: завтра можно будет поменять
        // на JdbcRepository и больше ничего не изменится
        UserRepository userRepo = new InMemoryUserRepository();
        WalletRepository walletRepo = new InMemoryWalletRepository();
        TransactionRepository transactionRepo = new InMemoryTransactionRepository();

        // Создаем пользователей
        User mia = new User("mia", "miaskam@icloud.com", "pass1234");
        User timur = new User("timur", "semenov-timur@icloud.com", "pass1234");
        userRepo.save(mia);
        userRepo.save(timur);

        System.out.println("Зарегестрировано пользователей: " + userRepo.count());
        System.out.println("Поиск mia: " + userRepo.findByUsername("mia").isPresent());
        System.out.println("Поиск kirill: " + userRepo.findByUsername("kirill").isPresent());

        // Создаем кошелек
        Wallet miaWallet = new Wallet(mia.getId(), "Кошелек Мии");
        Wallet miaBackupWallet = new Wallet(mia.getId(), "Запасной кошелек Мии");
        Wallet timurWallet = new Wallet(timur.getId(), "Кошелек Тимура");
        walletRepo.save(miaWallet);
        walletRepo.save(miaBackupWallet);
        walletRepo.save(timurWallet);

        System.out.println("\nКошельки Мии найдены по userId: " + walletRepo.findByUserId(mia.getId()));

        // Пополняем
        miaWallet.deposit(new BigDecimal("5000.00"));

        // Создание записи транзакции
        Transaction deposit = new Transaction.Builder(
                miaWallet.getId(),
                TransactionType.DEPOSIT,
                new BigDecimal("5000.00")
        )
                .decscription("Пополнение через банкомат")
                .build();
        transactionRepo.save(deposit);

        System.out.println("Баланс Мии после пополнения: " + miaWallet.getBalance() + " " + miaWallet.getCurrency());

        // ––– Перевод от Мии Тимуру –––
        BigDecimal transferAmount = new BigDecimal("1500.00");

        if (miaWallet.hasSufficientFunds(transferAmount)) {
            miaWallet.withdraw(transferAmount);
            timurWallet.deposit(transferAmount);

            Transaction out = new Transaction.Builder(miaWallet.getId(), TransactionType.TRANSACTION_OUT, transferAmount)
                    .counterpartyWalletId(timurWallet.getId())
                    .decscription("Перевод Тимуру")
                    .build();
            transactionRepo.save(out);

            Transaction in = new Transaction.Builder(timurWallet.getId(), TransactionType.TRANSACTION_IN, transferAmount)
                    .counterpartyWalletId(miaWallet.getId())
                    .decscription("Перевод от Мии")
                    .build();
            transactionRepo.save(in);

            System.out.println("Перевод выполнен: " + transferAmount + "RUB");
        }

        System.out.println("\nБаланс Мии: " + miaWallet.getBalance() + " " +  miaWallet.getCurrency());
        System.out.println("\nБаланс Тимура: " + timurWallet.getBalance() + " " +  timurWallet.getCurrency());

        List<Transaction> timurHistory = transactionRepo.findByWalletId(timurWallet.getId());
        System.out.println("История Тимура: " +  timurHistory.size() + " операций");
        for (Transaction transaction : timurHistory) {
            System.out.println(String.format("[%s] %s %s – %s\n",
                    transaction.getType(),
                    transaction.getAmount(),
                    timurWallet.getCurrency(),
                    transaction.getDescription()));
        }

        List<Transaction> miaHistory = transactionRepo.findByWalletId(miaWallet.getId());
        System.out.println("История переводов Мии: " +  miaHistory.size() + " операций");
        for (Transaction transaction : miaHistory) {
            System.out.println(String.format("[%s] %s %s – %s",
                    transaction.getType(),
                    transaction.getAmount(),
                    miaWallet.getCurrency(),
                    transaction.getDescription()));
        }

        // --- Общая статистика ---
        System.out.println("\n=== Статистика системы ===");
        System.out.println("Пользователей: " + userRepo.count());
        System.out.println("Кошельков: " + walletRepo.count());
        System.out.println("Транзакций: " + transactionRepo.count());

        System.out.println("\nВсе функции работают корректно!");
    }
}
