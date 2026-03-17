package com.paypocket.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Кошелек пользователя.
 *
 * Хранит текущий баланс. Баланс не может быть отрицательным.
 * Изменение баланса доступно только через контролируемые методы – withdraw() и deposit(),
 * а не через публичный сеттер.
 */
public class Wallet {

    private final UUID id;
    private final UUID userId;              // к какому пользователю привязан
    private String name;                    // "Основной", "Копилка" и т.д.
    private BigDecimal balance;
    private final String currency;          // "RUB" по умолчанию
    private final LocalDateTime createdAt;

    // ––– Конструкторы –––

    /**
     * Создание НОВОГО кошелька.
     */
    public Wallet(UUID userId, String name) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.balance = BigDecimal.ZERO;
        this.currency = "RUB";
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Восстановление существующего кошелька из хранилища.
     */
    public Wallet(UUID id, UUID userId, String name, BigDecimal balance, String currency, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.balance = balance;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    // ––– Бизнес-методы для изменения баланса

    /**
     * Пополнение кошелька.
     *
     * @param amount сумма пополнения (должна быть > 0)
     * @throws IllegalArgumentException если amount <= 0
     */
    public void deposit(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * Списание с кошелька.
     *
     * @param amount сумма списания (должна быть > 0)
     * @throws IllegalArgumentException если amount <= 0 или недостаточно средств
     */
    public void withdraw(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    String.format("Недостаточно средств. Баланс: %s, запрошено: %s",
                            this.balance, amount)
            );
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Проверка: достаточно ли средств для операции?
     */
    public boolean hasSufficientFunds(BigDecimal amount) {
        return  balance.compareTo(amount) >= 0;
    }

    // ––– Приватный вспомогательный метод –––

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    String.format("Сумма должна быть положительной, получено: %s",
                            amount)
            );
        }
    }

    // ––– Геттеры –––

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    // ––– Сеттеры: можно изменить только название кошелька –––

    public void setName(String name) {
        this.name = name;
    }

    // ––– equals / hashCode по id –––

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return Objects.equals(this.id, wallet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ––– toString –––

    @Override
    public String toString() {
        return "Wallet{" +
        "id=" + id + '\'' +
        ", userId=" + userId + '\'' +
        ", name=" + name + '\'' +
        ", balance=" + balance + '\'' +
        ", currency=" + currency + '\'' +
        '}';
    }

}
