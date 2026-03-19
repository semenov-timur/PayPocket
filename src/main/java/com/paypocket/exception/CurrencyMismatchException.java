package com.paypocket.exception;

import com.paypocket.model.Currency;

/**
 * Несоответствие валют.
 * Выбрасывается при попытке перевода средств между кошельками с разным типом валют.
 */
public class CurrencyMismatchException extends PayPocketException {

    public CurrencyMismatchException(Currency fromCurrency, Currency toCurrency) {
        super(String.format(
                "Перевод между разными валютами не поддерживается: %s -> %s.",
                fromCurrency.getSymbol(), toCurrency.getSymbol()
        ));
    }
}
