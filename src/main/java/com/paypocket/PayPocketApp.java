package com.paypocket;

import com.paypocket.model.*;

import java.math.BigDecimal;

/**
 * Точка входа в приложние PayPocket.
 */
public class PayPocketApp {
    public static void main(String[] args) {
        System.out.println("===== PayPocket – Digital Wallet =====\n");

        // Создаем пользователя
        User mia = new User("mia", "miaskam@icloud.com", "pass1234");
        System.out.println("Создан пользователь: " + mia);

        // Создаем кошелек
        Wallet wallet = new Wallet(mia.getId(), "Основной кошелек");
        System.out.println("Создан кошлек: " + wallet);

        // Пополняем
        wallet.deposit(new BigDecimal("1000.50"));
        System.out.println("Баланс после пополнения: " + wallet.getBalance());

        // Создание записи транзакции
        Transaction deposit = new Transaction.Builder(
                wallet.getId(),
                TransactionType.DEPOSIT,
                new BigDecimal("1000.50")
        )
                .decscription("Пополнение через банкомат")
                .build();
        System.out.println("Транзакция: " + deposit);

        // Проверяем списание
        wallet.withdraw(new BigDecimal("250.50"));
        System.out.println("Баланс после списания: " + wallet.getBalance());

        try {
            wallet.withdraw(new BigDecimal("99999.00"));
        }
        catch (Exception e) {
            System.out.println("Ожидаемая ошибка: " +  e.getMessage());
        }

        System.out.println("\nМодели работают корректно!");
    }
}
