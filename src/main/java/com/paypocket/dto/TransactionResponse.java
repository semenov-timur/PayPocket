package com.paypocket.dto;

import com.paypocket.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для получения транзакции.
 */
public class TransactionResponse {

    private String type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;

    /**
     * Фабричный метод.
     * Конвертирует Transaction в безопасный для API объект.
     */
    public static TransactionResponse from(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.type = transaction.getType().name();
        response.amount = transaction.getAmount();
        response.description = transaction.getDescription();
        response.createdAt = transaction.getCreatedAt();
        return response;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
