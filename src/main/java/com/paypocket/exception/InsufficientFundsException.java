package com.paypocket.exception;

import java.math.BigDecimal;

/**
 * Недостаточно средств для выполнения операции.
 */
public class InsufficientFundsException extends PayPocketException {

    private final BigDecimal balance;
    private final BigDecimal requested;

    public InsufficientFundsException(BigDecimal balance, BigDecimal requested) {
        super(String.format(
                "Недостаточно средств. Баланс: %s; Запрошено: %s", balance, requested
        ));
        this.balance = balance;
        this.requested = requested;
    }

    public  BigDecimal getBalance() {
        return balance;
    }

    public  BigDecimal getRequested() {
        return requested;
    }
}
