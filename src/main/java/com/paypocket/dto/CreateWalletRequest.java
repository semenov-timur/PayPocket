package com.paypocket.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для создания кошелька.
 */
public class CreateWalletRequest {

    @NotBlank(message = "Название кошелька обязательно")
    private String name;

    @NotBlank(message = "Валюта обязательна")
    private String currency;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
