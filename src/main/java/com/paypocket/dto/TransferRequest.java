package com.paypocket.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO для перевода.
 */
public class TransferRequest {

    @NotBlank(message = "Username получателя обязателен")
    private String recipientUsername;

    @NotNull(message = "Сумма перевода обязательна")
    @DecimalMin(value = "0.01", message = "Минимальная сумма: 0.01")
    @DecimalMax(value = "99999999999999999.99", message = "Максимальная сумма: 99,999,999,999,999,999.99")
    @Digits(integer = 17, fraction = 2, message = "Максимум два знака после запятой")
    private BigDecimal amount;

    public String getRecipientUsername() {
        return recipientUsername;
    }
    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
