package com.paypocket.repository.jdbc;

import com.paypocket.config.DatabaseConnectionManager;
import com.paypocket.model.User;
import com.paypocket.repository.UserRepository;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Хранение пользователей в БД PostgreSQL>.
 *
 * <p>Заменяет InMemoryUserRepository.
 * Реализует тот же интерфейс {@link UserRepository}, поэтому сервисный слой
 * не требует изменений.</p>
 *
 * @see UserRepository
 */
public class JdbcUserRepository extends AbstractJdbcRepository<User> implements UserRepository {

    public JdbcUserRepository(DatabaseConnectionManager databaseConnectionManager) {
        super(databaseConnectionManager);
    }

    @Override
    protected String getTableName() {
        return "users";
    }

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
    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT * FROM users WHERE LOWER(username) = LOWER(?);
        """;

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
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
    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getObject("id", UUID.class),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

}
