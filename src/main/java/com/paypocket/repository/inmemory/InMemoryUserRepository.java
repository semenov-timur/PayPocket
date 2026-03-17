package com.paypocket.repository.inmemory;

import com.paypocket.model.User;
import com.paypocket.repository.UserRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранение пользователей в оперативной памяти.
 *
 * <p>Данные живут только пока работает приложение.
 * Используется на первоначальном этапе.
 * Позже будет заменен на {@code JdbcUserRepository}.</p>
 *
 * @see UserRepository
 */
public class InMemoryUserRepository implements UserRepository {

    // Хэш-таблица для хранения пользователей (UUID -> User) с потокобезопасностью
    private final Map<UUID, User> storage = new ConcurrentHashMap<>();

    @Override
    public User save(User entity) {
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<User> findAll() {
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

    // ––– Специфичные методы UserRepository –––

    @Override
    public Optional<User> findByUsername(String username) {
        return storage.values().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        return storage.values().stream()
                .anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
    }

    @Override
    public boolean existsByEmail(String email) {
        return storage.values().stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }
}
