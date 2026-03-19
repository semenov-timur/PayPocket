package com.paypocket.model;

/**
 * Поддерживаемые валюты.
 */
public enum Currency {
    RUB("₽", "Российский рубль"),
    USD("$", "Доллар США"),
    EUR("€", "Евро");

    private final String symbol;
    private final String description;

    Currency(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

    public static Currency fromString(String value) {
        try {
            return Currency.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
