package com.paypocket.dto;

import com.paypocket.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для получения кошелька.
 */
public class WalletResponse {
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
        response.name =  wallet.getName();
        response.balance =  wallet.getBalance();
        response.currency = wallet.getCurrency().name();
        response.createdAt = wallet.getCreatedAt();
        return response;
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
