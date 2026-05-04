package com.paypocket.exception;

import java.util.UUID;

/**
 * Кошельки принадлежат разным пользователям.
 *
 * <p>Выбрасывается при попытке выполнить операцию (например, конвертацию),
 * которая допустима только между кошельками одного и того же владельца.</p>
 */
public class WalletOwnershipException extends PayPocketException {

    public WalletOwnershipException(UUID fromWalletId, UUID toWalletId) {
        super(String.format(
                "Кошельки принадлежат разным пользователям: %s и %s.",
                fromWalletId, toWalletId
        ));
    }
}