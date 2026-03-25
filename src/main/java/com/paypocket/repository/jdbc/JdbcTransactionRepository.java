package com.paypocket.repository.jdbc;

import com.paypocket.config.DatabaseConnectionManager;
import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;
import com.paypocket.repository.TransactionRepository;
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
 * Хранение транзакций в БД PostgreSQL.
 *
 * <p>Заменяет {@link com.paypocket.repository.inmemory.InMemoryTransactionRepository}
 *  Реализует тот же интерфейс {@link TransactionRepository}, поэтому сервисный слой
 *  не требует изменений.</p>
 *
 * @see UserRepository
 */
public class JdbcTransactionRepository implements TransactionRepository {

    private final DatabaseConnectionManager connectionManager;

    public JdbcTransactionRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // ======================================================
    // CRUD - базовые операции
    // ======================================================

    @Override
    public Transaction save(Transaction transaction) {
        String sql = """
                INSERT INTO transactions
                    (id, wallet_id, counterparty_wallet_id, type, amount, description, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;

        try (Connection connection = connectionManager.getConnection();
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

    @Override
    public Optional<Transaction> findById(UUID id) {
        String sql = """
                SELECT * FROM transactions WHERE id = ?;
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTransaction(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске транзакции: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findAll() {
        String sql = """
                SELECT * FROM transactions ORDER BY created_at DESC;
                """;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                transactions.add(mapRowToTransaction(rs));
            }
            return transactions;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения списка транзакций: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        String sql = """
                SELECT 1 FROM transactions WHERE id = ?;
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при проверке существования транзакции: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        String sql = """
                REMOVE FROM transactions WHERE id = ?;
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении транзакции: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = """
                SELECT COUNT(*) FROM transactions;
                """;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return (rs.next() ? rs.getLong(1) : 0);
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при подсчете транзакций: " + e.getMessage(), e);
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
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, walletId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRowToTransaction(rs));
                }
                return transactions;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка : " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type) {
        String sql = """
                SELECT * FROM transactions WHERE wallet_id = ? AND type = ? ORDER BY created_at DESC;
                """;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, walletId);
            stmt.setString(2, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRowToTransaction(rs));
                }
                return transactions;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка : " + e.getMessage(), e);
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
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRowToTransaction(rs));
                }
                return transactions;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка : " + e.getMessage(), e);
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
    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID walletId = rs.getObject("wallet_id", UUID.class);
        UUID counterpartyWalletId = rs.getObject("counterparty_wallet_id", UUID.class);
        TransactionType type = TransactionType.valueOf(rs.getString("type"));
        BigDecimal amount = rs.getBigDecimal("amount");
        String description = rs.getString("description");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new Transaction.Builder(walletId, type, amount)
                .id(id)
                .counterpartyWalletId(counterpartyWalletId)
                .decscription(description)
                .createdAt(createdAt)
                .build();
    }
}
