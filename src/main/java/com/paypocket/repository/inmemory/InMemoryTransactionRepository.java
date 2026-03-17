package com.paypocket.repository.inmemory;

import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.UserRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранение транзакций в оперативной памяти.
 *
 * <p>Данные живут только пока работает приложение.
 * Используется на первоначальном этапе.
 * Позже будет заменен на {@code JdbcUserRepository}.</p>
 *
 * <p>Транзакции — иммутабельные объекты, поэтому после сохранения
 * не изменяются. Метод save() используется только для добавления новых.</p>
 *
 * @see UserRepository
 */
public class InMemoryTransactionRepository implements TransactionRepository {

    // Хэш-таблица для хранения транзакций (UUID -> Transaction) с потокобезопасностью
    private final Map<UUID, Transaction> storage = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction entity) {
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean existsById(UUID id) {
        return storage.containsKey(id);
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    // ––– Специфичные методы TransactionRepository –––

    @Override
    public List<Transaction> findByWalletId(UUID walletId) {
        return storage.values().stream()
                .filter(transaction -> transaction.getWalletId().equals(walletId))
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed()) // сначала новые
                .toList();
    }

    @Override
    public List<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type) {
        return storage.values().stream()
                .filter(transaction -> transaction.getWalletId().equals(walletId))
                .filter(transaction -> transaction.getType() == type)
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed()) // сначала новые
                .toList();
    }
}
