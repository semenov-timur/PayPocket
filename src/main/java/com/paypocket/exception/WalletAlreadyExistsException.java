package com.paypocket.exception;

import com.paypocket.model.Currency;

import java.util.UUID;

/**
 * Кошелек уже существует.
 * Выбрасывается при попытке создать кошелек с такой валютой.
 */
public class WalletAlreadyExistsException extends PayPocketException {

    public  WalletAlreadyExistsException(Currency currency) {
        super(String.format(
                "У вас уже есть кошелек в валюте: %s", currency.getDescription()
        ));
    }
}
