package com.paypocket.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Результат операции перевода.
 *
 * <p>DTO (Data Transfer Object) — содержит информацию о выполненном переводе
 * для передачи в UI-слой. Не является сущностью БД.</p>
 */
public class TransferResult {

    private final UUID fromWalletId;
    private final UUID toWalletId;
    private final BigDecimal amount;
    private final BigDecimal senderBalanceAfter;
    private final BigDecimal receiverBalanceAfter;

    public TransferResult(UUID fromWalletId, UUID toWalletId, BigDecimal amount, BigDecimal senderBalanceAfter, BigDecimal receiverBalanceAfter) {
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.senderBalanceAfter = senderBalanceAfter;
        this.receiverBalanceAfter = receiverBalanceAfter;
    }

    public UUID getFromWalletId() {
        return fromWalletId;
    }

    public UUID getToWalletId() {
        return toWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getSenderBalanceAfter() {
        return senderBalanceAfter;
    }

    public BigDecimal getReceiverBalanceAfter() {
        return receiverBalanceAfter;
    }

    @Override
    public String toString() {
        return String.format(
                "Перевод: %s -> %s, сумма: %s, баланс отправителя: %s, баланс получателя: %s",
                fromWalletId, toWalletId, amount, senderBalanceAfter, receiverBalanceAfter
        );
    }
}
