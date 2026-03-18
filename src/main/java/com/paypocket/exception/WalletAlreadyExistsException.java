package com.paypocket.exception;

import java.util.UUID;

/**
 * Кошелек уже существует.
 * Выбрасывается при попытке создать кошелек с такой валютой.
 */
public class WalletAlreadyExistsException extends PayPocketException {

    public  WalletAlreadyExistsException(UUID userId, String currency) {
        super(String.format(
                "У пользователя %s уже есть кошелек в валюте %s", userId, currency
        ));
    }
}
