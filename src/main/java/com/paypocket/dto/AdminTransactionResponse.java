package com.paypocket.dto;

import com.paypocket.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO транзакции для администратора.
 *
 * <p>В отличие от {@link TransactionResponse} (история одного кошелька),
 * здесь дополнительно отдаём {@code walletId} и {@code counterpartyWalletId} —
 * администратор смотрит транзакции по всем кошелькам сразу, поэтому ему
 * важно видеть, к каким кошелькам относится операция.</p>
 */
public class AdminTransactionResponse {

    private UUID id;
    private UUID walletId;
    private UUID counterpartyWalletId;
    private String type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;

    public static AdminTransactionResponse from(Transaction transaction) {
        AdminTransactionResponse response = new AdminTransactionResponse();
        response.id = transaction.getId();
        response.walletId = transaction.getWalletId();
        response.counterpartyWalletId = transaction.getCounterpartyWalletId();
        response.type = transaction.getType().name();
        response.amount = transaction.getAmount();
        response.description = transaction.getDescription();
        response.createdAt = transaction.getCreatedAt();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getCounterpartyWalletId() {
        return counterpartyWalletId;
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
