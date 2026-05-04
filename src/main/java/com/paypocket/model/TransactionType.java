package com.paypocket.model;

/**
 * Типы операций в системе.
 * <p>
 * DEPOSIT          – пополнение кошелька
 * WITHDRAW         – снятие средств
 * TRANSACTION_IN   – Входящий перевод от другого пользователя
 * TRANSACTION_OUT  – Исходящий перевод другому пользователю
 * CONVERT_IN       – Зачисление в результате конвертации валют
 * CONVERT_OUT      – Списание в результате конвертации валют
 */
public enum TransactionType {
    DEPOSIT("Пополнение"),
    WITHDRAW("Снятие"),
    TRANSACTION_IN("Входящий перевод"),
    TRANSACTION_OUT("Исходящий перевод"),
    CONVERT_IN("Зачисление по конвертации"),
    CONVERT_OUT("Списание по конвертации");

    private String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}
