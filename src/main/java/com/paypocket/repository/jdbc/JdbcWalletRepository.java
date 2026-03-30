package com.paypocket.repository.jdbc;

import com.paypocket.config.DatabaseConnectionManager;
import com.paypocket.model.Currency;
import com.paypocket.model.Wallet;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Хранение кошельков в БД PostgreSQL.
 *
 * <p>Заменяет InMemoryWalletRepository.
 *  Реализует тот же интерфейс {@link WalletRepository}, поэтому сервисный слой
 *  не требует изменений.</p>
 *
 * @see UserRepository
 */
public class JdbcWalletRepository extends AbstractJdbcRepository<Wallet> implements WalletRepository {

    public  JdbcWalletRepository(DatabaseConnectionManager databaseConnectionManager) {
        super(databaseConnectionManager);
    }

    @Override
    protected String getTableName() {
        return "wallets";
    }

    // ======================================================
    // CRUD - операции
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

        try (Connection connection = databaseConnectionManager.getConnection();
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

    /**
     * Находит кошелек с блокирровкой строки (SELECT FOR UPDATE).
     * Используется внутри транзакции для предотвращения
     * конкурентных изменений баланса.
     *
     * @param conn соединение с активной транзакцией
     * @param walletId id кошелька
     * @return найденный кошелек или пустой Optional
     */
    @Override
    public Optional<Wallet> findByIdForUpdate(Connection conn, UUID walletId) throws SQLException {
        String sql = """
                SELECT * FROM wallets WHERE id = ?;
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, walletId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
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
        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    wallets.add(mapRow(rs));
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
    @Override
    protected Wallet mapRow(ResultSet rs) throws SQLException {
        return new Wallet(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("name"),
                rs.getBigDecimal("balance"),
                Currency.valueOf(rs.getString("currency")),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
