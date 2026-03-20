package com.paypocket.exception;

import java.util.UUID;

/**
 * Попытка пересести деньги самому себе (на тот же кошелек).
 */
public class SelfTransferException extends PayPocketException {

    public SelfTransferException(UUID walletId) {
        super("Нельзя перевести средства на тот же кошелек");
    }
}
