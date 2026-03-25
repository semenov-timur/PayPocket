package com.paypocket.repository.jdbc;

import com.paypocket.config.DatabaseConnectionManager;
import com.paypocket.model.Currency;
import com.paypocket.model.Wallet;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Хранение кошельков в БД PostgreSQL.
 *
 * <p>Заменяет {@link com.paypocket.repository.inmemory.InMemoryWalletRepository}
 *  Реализует тот же интерфейс {@link WalletRepository}, поэтому сервисный слой
 *  не требует изменений.</p>
 *
 * @see UserRepository
 */
public class JdbcWalletRepository implements WalletRepository {

    private final DatabaseConnectionManager connectionManager;

    public  JdbcWalletRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // ======================================================
    // CRUD - базовые операции
    // ======================================================

    @Override
    public Wallet save(Wallet wallet) {
        String sql = """
                INSERT INTO wallets (id, user_id, name, balance, currency, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    balance = EXCLUDED.balance
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, wallet.getId());
            stmt.setObject(2, wallet.getUserId());
            stmt.setString(3, wallet.getName());
            stmt.setBigDecimal(4, wallet.getBalance());
            stmt.setString(5, wallet.getCurrency().name());
            stmt.setTimestamp(6, Timestamp.valueOf(wallet.getCreatedAt()));

            stmt.executeUpdate();
            return wallet;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения кошелька: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        String sql = """
                SELECT * FROM wallets WHERE id = ?;
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowsToWallet(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска кошелька: " + e.getMessage(), e);
        }
    }

    /**
     * Находит кошелек с блокирровкой строки (SELECT FOR UPDATE).
     * Используется внутри транзакции для предотвращения
     * конкурентных изменений баланса.
     *
     * @param conn соединение с активной транзакцией
     * @param walletId id кошелька
     * @return найденный кошелек или пустой Optional
     */
    public Optional<Wallet> findByIdForUpdate(Connection conn, UUID walletId) throws SQLException {
        String sql = """
                SELECT * FROM wallets WHERE id = ?;
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, walletId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowsToWallet(rs));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<Wallet> findAll() {
        String sql = """
                SELECT * FROM wallets;
                """;
        List<Wallet> wallets = new ArrayList<>();

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            try  (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    wallets.add(mapRowsToWallet(rs));
                }
                return wallets;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения списка кошельков: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        String sql = """
                SELECT 1 FROM wallets WHERE id = ?;
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при проверке существования кошелька: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        String sql = """
                DELETE FROM wallets WHERE id = ?;
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении кошелька: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = """
                SELECT COUNT(*) FROM wallets;
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return (rs.next() ? rs.getLong(1) : 0);
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при подсчёте кошельков: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // Специфичный метод WalletRepository
    // ================================================================

    @Override
    public List<Wallet> findByUserId(UUID userId) {
        String sql = """
                SELECT * FROM wallets WHERE user_id = ?;
                """;
        List<Wallet> wallets = new ArrayList<>();
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    wallets.add(mapRowsToWallet(rs));
                }
                return wallets;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске кошельков пользователя: " + e.getMessage(), e);
        }
    }

    /**
     * Обновляет баланс кошелька в рамках существующей транзакции.
     *
     * @param conn соединение с активной транзакцией
     * @param walletId id кошелька
     * @param newBalance обновлненный баланс
     */
    public void updateBalance(Connection conn, UUID walletId, BigDecimal newBalance) throws SQLException {
        String sql = """
                UPDATE wallets SET balance = ? WHERE id = ?;
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setObject(2, walletId);
            stmt.executeUpdate();
        }
    }

    // ================================================================
    // Map ResultSet -> Wallet
    // ================================================================

    /**
     * Преобразует текущую строку ResultSet в объект Wallet.
     *
     * <p>Используется во всех методах, возвращающих Wallet — это DRY-принцип.
     * Столбцы читаются по имени (не по номеру) — так надёжнее,
     * потому что порядок столбцов в SELECT может меняться.</p>
     */
    private Wallet mapRowsToWallet(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID  userId = rs.getObject("user_id", UUID.class);
        String name = rs.getString("name");
        BigDecimal balance = rs.getBigDecimal("balance");
        Currency currency = Currency.valueOf(rs.getString("currency"));
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new Wallet(id, userId, name, balance, currency, createdAt);
    }
}
