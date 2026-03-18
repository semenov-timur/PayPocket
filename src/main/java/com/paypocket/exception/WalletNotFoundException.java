package com.paypocket.exception;

import java.util.UUID;

/**
 * Кошелек не найден.
 * Выбрасывается при попытке найти несуществующий кошелек.
 */
public class WalletNotFoundException extends PayPocketException {

    private final UUID walletId;

    public WalletNotFoundException(UUID walletId) {
        super(String.format("Кошелек не найден: " + walletId.toString()));
        this.walletId = walletId;
    }

    public UUID getWalletId() {
        return walletId;
    }
}
