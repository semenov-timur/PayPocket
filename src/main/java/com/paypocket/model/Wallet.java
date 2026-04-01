package com.paypocket.model;

import com.paypocket.exception.InsufficientFundsException;
import com.paypocket.exception.InvalidAmountException;
import jakarta.persistence.*;

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
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;              // к какому пользователю привязан

    @Column(name = "name", nullable = false, length = 100)
    private String name;                    // "Основной", "Копилка" и т.д.

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency",  nullable = false,  length = 3)
    private Currency currency;          // "RUB" по умолчанию

    @Column(name = "created_at",  nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ––– Конструкторы –––

    /**
     * Конструктор без аргументов.
     */
    protected Wallet() {}

    /**
     * Создание НОВОГО кошелька с выбором валюты.
     */
    public Wallet(UUID userId, String name, Currency currency) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.balance = BigDecimal.ZERO;
        this.currency = currency;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Создание НОВОГО рублевого кошелька по умолчанию.
     */
    public Wallet(UUID userId, String name) {
        this(userId, name, Currency.RUB);
    }

    /**
     * Восстановление существующего кошелька из хранилища.
     */
    public Wallet(UUID id, UUID userId, String name, BigDecimal balance, Currency currency, LocalDateTime createdAt) {
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
            throw new InsufficientFundsException(this.balance, amount);
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
            throw new InvalidAmountException(amount);
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

    public Currency getCurrency() {
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
