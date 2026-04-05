package com.paypocket.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO для пополнения.
 */
public class DepositRequest {

    @NotNull(message = "Сумма пополнения обязательна")
    @DecimalMin(value = "0.01", message = "Минимальная сумма: 0.01")
    @DecimalMax(value = "99999999999999999.99", message = "Максимальная сумма: 99,999,999,999,999,999.99")
    @Digits(integer = 17, fraction = 2, message = "Максимум два знака после запятой")
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
