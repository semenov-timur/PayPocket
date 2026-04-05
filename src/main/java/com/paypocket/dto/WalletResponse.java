package com.paypocket.dto;

import com.paypocket.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO для получения кошелька.
 */
public class WalletResponse {
    private UUID id;
    private String name;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;

    /**
     * Фабричный метод.
     * Конвертирует Wallet в безопасный для API объект.
     */
    public static  WalletResponse from(Wallet wallet) {
        WalletResponse response = new WalletResponse();
        response.id = wallet.getId();
        response.name =  wallet.getName();
        response.balance =  wallet.getBalance();
        response.currency = wallet.getCurrency().name();
        response.createdAt = wallet.getCreatedAt();
        return response;
    }

    public UUID getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public BigDecimal getBalance() {
        return balance;
    }
    public String getCurrency() {
        return currency;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
