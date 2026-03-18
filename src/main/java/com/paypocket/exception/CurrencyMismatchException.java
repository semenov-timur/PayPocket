package com.paypocket.exception;

/**
 * Несоответствие валют.
 * Выбрасывается при попытке перевода средств между кошельками с разным типом валют.
 */
public class CurrencyMismatchException extends PayPocketException {

    public CurrencyMismatchException(String fromCurrency, String toCurrency) {
        super(String.format(
                "Перевод между разными валютами не поддерживается: %s -> %s.",
                fromCurrency, toCurrency
        ));
    }
}
