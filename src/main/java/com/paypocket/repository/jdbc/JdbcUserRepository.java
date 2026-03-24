package com.paypocket.repository.jdbc;

import com.paypocket.config.DatabaseConnectionManager;
import com.paypocket.model.User;
import com.paypocket.repository.UserRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Хранение пользователей в БД PostgrSQL>.
 *
 * <p>Заменяет {@link com.paypocket.repository.inmemory.InMemoryUserRepository}
 * Реализует тот же интерфейс {@link UserRepository}, плэтому сервисный слой
 * не требует изменений.</p>
 *
 * @see UserRepository
 */
public class JdbcUserRepository implements UserRepository {

    private final DatabaseConnectionManager databaseConnectionManager;

    public JdbcUserRepository(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = databaseConnectionManager;
    }

    // ======================================================
    // CRUD - базовые операции
    // ======================================================

    @Override
    public User save(User user) {
        String sql = """
                INSERT INTO users (id, username, email, password, created_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    username = EXCLUDED.USERNAME,
                    email = EXCLUDED.EMAIL,
                    password = EXCLUDED.PASSWORD
                """;

        try (Connection connection = databaseConnectionManager.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setTimestamp(5, Timestamp.valueOf(user.getCreatedAt()));
            stmt.executeUpdate();
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения пользователя: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        String sql = """
                SELECT * FROM users WHERE id = ?;
                """;

        try (Connection connection = databaseConnectionManager.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = """
                SELECT * FROM users;
        """;
        List<User> users = new ArrayList<>();
        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка пользователей: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        String  sql = """
                SELECT * FROM users WHERE id = ?;
        """;

        try (Connection connection = databaseConnectionManager.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, id);
            try  (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки существования: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        String sql = """
                DELETE FROM users WHERE id = ?;
        """;

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления пользователя: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = """
                SELECT COUNT(*) FROM users;
        """;

        try (Connection connection = databaseConnectionManager.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при подсчете пользователей: " + e.getMessage(), e);
        }
    }

    // ======================================================
    // Специфичные методы UserRepository
    // ======================================================

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT * FROM users WHERE LOWER(username) = LOWER(?);
        """;

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении пользователя по username: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = """
                SELECT 1 FROM users WHERE LOWER(username) = LOWER(?);
        """;

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return  rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки username: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = """
                SELECT 1 FROM users WHERE LOWER(email) = LOWER(?);
        """;

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return  rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки username: " + e.getMessage(), e);
        }
    }

    // ======================================================
    // Map ResultSet -> User
    // ======================================================

    /**
     * Преобразует текущую строку ResultSet в объект User.
     *
     * <p>Используется во всех методах, возвращающих User — это DRY-принцип.
     * Столбцы читаются по имени (не по номеру) — так надёжнее,
     * потому что порядок столбцов в SELECT может меняться.</p>
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        UUID uuid = rs.getObject("id", UUID.class);
        String username = rs.getString("username");
        String email = rs.getString("email");
        String password = rs.getString("password");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new User(uuid, username, email, password, createdAt);
    }

}
