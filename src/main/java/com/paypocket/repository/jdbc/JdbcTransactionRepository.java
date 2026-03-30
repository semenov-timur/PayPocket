package com.paypocket.repository.jdbc;

import com.paypocket.config.DatabaseConnectionManager;
import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.UserRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Хранение транзакций в БД PostgreSQL.
 *
 * <p>Заменяет InMemoryTransactionRepository.
 *  Реализует тот же интерфейс {@link TransactionRepository}, поэтому сервисный слой
 *  не требует изменений.</p>
 *
 * @see UserRepository
 */
public class JdbcTransactionRepository extends AbstractJdbcRepository<Transaction> implements TransactionRepository {

    public JdbcTransactionRepository(DatabaseConnectionManager databaseConnectionManager) {
        super(databaseConnectionManager);
    }

    @Override
    protected String getTableName() {
        return "transactions";
    }

    // ======================================================
    // CRUD - операции
    // ======================================================

    @Override
    public Transaction save(Transaction transaction) {
        String sql = """
                INSERT INTO transactions
                    (id, wallet_id, counterparty_wallet_id, type, amount, description, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, transaction.getId());
            stmt.setObject(2, transaction.getWalletId());
            if (transaction.getCounterpartyWalletId() != null) {
                stmt.setObject(3, transaction.getCounterpartyWalletId());
            }
            else {
                stmt.setNull(3, Types.OTHER);
            }
            stmt.setString(4, transaction.getType().name());
            stmt.setBigDecimal(5, transaction.getAmount());
            stmt.setString(6, transaction.getDescription());
            stmt.setTimestamp(7, Timestamp.valueOf(transaction.getCreatedAt()));

            stmt.executeUpdate();
            return transaction;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения транзакции: " + e.getMessage(), e);
        }
    }

    /**
     * Сохраняет транзакцию в рамках текущего соединения (транзакции).
     *
     * @param conn          соединение с активной транзакцией
     * @param transaction   транзакция для сохранения
     */
    public void save(Connection conn, Transaction transaction) throws SQLException {
        String sql = """
                INSERT INTO transactions
                    (id, wallet_id, counterparty_wallet_id, type, amount, description, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, transaction.getId());
            stmt.setObject(2, transaction.getWalletId());
            if (transaction.getCounterpartyWalletId() != null) {
                stmt.setObject(3, transaction.getCounterpartyWalletId());
            }
            else {
                stmt.setNull(3, Types.OTHER);
            }
            stmt.setString(4, transaction.getType().name());
            stmt.setBigDecimal(5, transaction.getAmount());
            stmt.setString(6, transaction.getDescription());
            stmt.setTimestamp(7, Timestamp.valueOf(transaction.getCreatedAt()));

            stmt.executeUpdate();
        }
    }

    // ================================================================
    // Специфичные методы TransactionRepository
    // ================================================================

    @Override
    public List<Transaction> findByWalletId(UUID walletId) {
        String sql = """
                SELECT * FROM transactions WHERE wallet_id = ? ORDER BY created_at DESC;
                """;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, walletId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
                return transactions;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка транзакций: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByWalletId(UUID walletId, int pageNumber, int pageSize) {
        String sql = """
                SELECT * FROM transactions
                WHERE wallet_id = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?;
                """;
        int offset = (pageNumber - 1) * pageSize;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, walletId);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
                return transactions;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения страницы транзакций: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type) {
        String sql = """
                SELECT * FROM transactions WHERE wallet_id = ? AND type = ? ORDER BY created_at DESC;
                """;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, walletId);
            stmt.setString(2, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
                return transactions;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения транзакций типа " + type.name() +  ": " + e.getMessage(), e);
        }
    }

    public List<Transaction> findByUsername(String username) {
        String sql = """
                SELECT t.* FROM transactions t
                JOIN wallets w ON t.wallet_id = w.id
                JOIN users u ON w.user_id = u.id
                WHERE LOWER(u.username) = LOWER(?)
                ORDER BY t.created_at DESC
                """;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
                return transactions;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения транзакций пользователя: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // Map ResultSet -> Wallet
    // ================================================================

    /**
     * Преобразует текущую строку ResultSet в объект Transaction.
     *
     * <p>Используется во всех методах, возвращающих Transaction — это DRY-принцип.
     * Столбцы читаются по имени (не по номеру) — так надёжнее,
     * потому что порядок столбцов в SELECT может меняться.</p>
     */
    @Override
    protected Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction.Builder(
                rs.getObject("wallet_id", UUID.class),
                TransactionType.valueOf(rs.getString("type")),
                rs.getBigDecimal("amount"))
                .id(rs.getObject("id", UUID.class))
                .counterpartyWalletId(rs.getObject("counterparty_wallet_id", UUID.class))
                .decscription(rs.getString("description"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }
}
