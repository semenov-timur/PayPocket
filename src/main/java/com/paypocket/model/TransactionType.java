package com.paypocket.model;

/**
 * Типы операций в системе.
 *
 * DEPOSIT          – пополнение кошелька
 * WITHDRAW         – снятие средств
 * TRANSACTION_IN   – Входящий перевод от другого пользователя
 * TRANSACTION_OUT  – Исходящий перевод другому пользователю
 */
public enum TransactionType {
    DEPOSIT("Пополнение"),
    WITHDRAW("Снятие"),
    TRANSACTION_IN("Входящий перевод"),
    TRANSACTION_OUT("Исходящий перевод");

    private String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}
