package com.paypocket.repository;

import com.paypocket.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий пользователей.
 *
 * <p>Расширяет базовый {@link Repository} методами поиска
 * по username и проверки уникальности.</p>
 */
public interface UserRepository extends Repository<User, UUID> {

    /**
     * Ищет пользователя по username.
     *
     * @param username имя пользователя
     * @return Optional с пользователем или пустой Optional
     */
    Optional<User> findByUsername(String username);

    /**
     * Проверяет, занят ли username&
     *
     * @param username имя пользователя
     * @return true, если занят
     */
    boolean existsByUsername(String username);

    /**
     * Проверяет, занят ли email.
     *
     * @param email email
     * @return true, если занят
     */
    boolean existsByEmail(String email);
}
