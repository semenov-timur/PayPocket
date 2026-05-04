package com.paypocket.dto;

import com.paypocket.model.Currency;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Результат операции конвертации валют.
 *
 * <p>В отличие от {@link TransferResult} содержит две суммы (списанную и зачисленную)
 * и применённый обменный курс — данные нужны UI для отображения деталей операции.</p>
 */
public class ConversionResult {

    private final UUID fromWalletId;
    private final UUID toWalletId;
    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final BigDecimal fromAmount;
    private final BigDecimal toAmount;
    private final BigDecimal rate;
    private final BigDecimal fromBalanceAfter;
    private final BigDecimal toBalanceAfter;

    public ConversionResult(UUID fromWalletId,
                            UUID toWalletId,
                            Currency fromCurrency,
                            Currency toCurrency,
                            BigDecimal fromAmount,
                            BigDecimal toAmount,
                            BigDecimal rate,
                            BigDecimal fromBalanceAfter,
                            BigDecimal toBalanceAfter) {
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.rate = rate;
        this.fromBalanceAfter = fromBalanceAfter;
        this.toBalanceAfter = toBalanceAfter;
    }

    public UUID getFromWalletId() {
        return fromWalletId;
    }

    public UUID getToWalletId() {
        return toWalletId;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public BigDecimal getFromAmount() {
        return fromAmount;
    }

    public BigDecimal getToAmount() {
        return toAmount;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getFromBalanceAfter() {
        return fromBalanceAfter;
    }

    public BigDecimal getToBalanceAfter() {
        return toBalanceAfter;
    }

    @Override
    public String toString() {
        return String.format(
                "Конвертация: %s %s -> %s %s (курс: %s), баланс отправителя: %s, баланс получателя: %s",
                fromAmount, fromCurrency.getSymbol(),
                toAmount, toCurrency.getSymbol(),
                rate, fromBalanceAfter, toBalanceAfter
        );
    }
}