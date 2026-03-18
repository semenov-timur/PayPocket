package com.paypocket.exception;

import java.math.BigDecimal;

/**
 * Некорректная сумма операции (отрицательная, ноль, null).
 */
public class InvalidAmountException extends PayPocketException {

    public InvalidAmountException(BigDecimal amount) {
        super(String.format(
                "Некорректная сумма операции: %s. Сумма должна быть больше нуля."
        ));
    }
}
