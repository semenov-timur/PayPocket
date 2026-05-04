package com.paypocket.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Запись об операции в кошельке.
 * <p>
 * Транзакция – иммутабельный объект, после создание не меняется.
 * Все поля – private final, нет сеттеров. Создается через {@link Builder}.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;                // кошелек владельца операции

    @Column(name = "counterparty_wallet_id")
    private UUID counterpartyWalletId;    // кошелек контрагента (null если DEPOSIT/WITHDRAW)

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "description")
    private String description;            // описание операции (опционально)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
    }

    // Приватный конструктор для создания через Builder

    /**
     * Конструктор через Builder (основоной).
     */
    private Transaction(Builder builder) {
        this.id = builder.id;
        this.walletId = builder.walletId;
        this.counterpartyWalletId = builder.counterpartyWalletId;
        this.type = builder.type;
        this.amount = builder.amount;
        this.description = builder.decription;
        this.createdAt = builder.createdAt;
    }

    // ––– Геттеры (нет сеттеров, класс иммутабельный) –––

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getCounterpartyWalletId() {
        return counterpartyWalletId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ––– equals / hashCode по id –––

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ––– toString –––

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", type=" + type +
                ", amount=" + amount +
                ", walletId=" + walletId +
                ", counterpartyWalletId=" + counterpartyWalletId +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    // =========================================================
    // BUILDER
    // =========================================================

    /**
     * Класс для создания иммутабельных объектов {@link Transaction}.
     * Реализует паттерн создания Builder.
     */
    public static class Builder {
        // Обязательные поля
        private UUID walletId;
        private TransactionType type;
        private BigDecimal amount;

        // Автоматические поля
        private UUID id = UUID.randomUUID();
        private LocalDateTime createdAt = LocalDateTime.now();

        // Опциональные поля
        private UUID counterpartyWalletId;
        private String decription;

        /**
         * Builder с обязательными полями,
         * без которых транзакция не имеет смысла.
         */
        public Builder(UUID walletId, TransactionType type, BigDecimal amount) {
            this.walletId = walletId;
            this.type = type;
            this.amount = amount;
        }

        /**
         * ID – для восстановления из хранилища.
         * По умолчанию генерируется автоматически.
         */
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder counterpartyWalletId(UUID counterpartyWalletId) {
            this.counterpartyWalletId = counterpartyWalletId;
            return this;
        }

        public Builder decscription(String decription) {
            this.decription = decription;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Создает иммутабельный объект Transaction.
         * Можно добавить валидацию перед созданием.
         */
        public Transaction build() {
            if ((type == TransactionType.TRANSACTION_IN
                    || type == TransactionType.TRANSACTION_OUT
                    || type == TransactionType.CONVERT_IN
                    || type == TransactionType.CONVERT_OUT)
                    && counterpartyWalletId == null) {
                throw new IllegalStateException("Для перевода/конвертации необходимо указать counterpartyWalletId");
            }
            return new Transaction(this);
        }
    }
}
