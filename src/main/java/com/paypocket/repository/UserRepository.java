package com.paypocket.repository;

import com.paypocket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий пользователей.
 *
 * <p>Наследует JpaRepository, а значит Spring самостоятельно
 * сгенерирует реализацию всех методов по имени.</p>
 *
 * <p>Основные CRUD-методы уже есть в базовом репозитории,
 * здесь только специфичные запросы.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Ищет пользователя по username.
     *
     * Spring генерирует: SELECT * FROM users WHERE LOWER(username) = LOWER(?)
     *
     * @param username имя пользователя
     * @return Optional с пользователем или пустой Optional
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Проверяет, занят ли username?
     *
     * <p>Spring генерирует: SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
     *                    FROM users WHERE LOWER(username) = LOWER(?)</p>
     *
     * @param username имя пользователя
     * @return true, если занят
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Проверяет, занят ли email.
     *
     * @param email email
     * @return true, если занят
     */
    boolean existsByEmail(String email);
}
