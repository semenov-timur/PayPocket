package com.paypocket.repository.inmemory;

import com.paypocket.model.Wallet;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранение кошельков в оперативной памяти.
 *
 * <p>Данные живут только пока работает приложение.
 * Используется на первоначальном этапе.
 * Позже будет заменен на {@code JdbcUserRepository}.</p>
 *
 * @see UserRepository
 */
public class InMemoryWalletRepository implements WalletRepository {

    // Хэш-таблица для хранения кошельков (UUID -> Wallet) с потокобезопасностью
    private final Map<UUID, Wallet> storage = new ConcurrentHashMap<>();

    @Override
    public Wallet save(Wallet entity) {
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return  Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Wallet> findAll() {
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

    // ––– Специфичные методы WalletRepository –––

    @Override
    public List<Wallet> findByUserId(UUID userId) {
        return storage.values().stream()
                .filter(wallet -> wallet.getUserId().equals(userId))
                .toList();
    }
}
