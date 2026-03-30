package com.paypocket.repository.jdbc;

import com.paypocket.config.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Базовый JDBC-репозиторий с общими CRUD-операциями.
 *
 * <p>Содержит реализации одинаковых для всех сущностей методов.
 * От него наследуются конкретные репозитории идобавляют свою специфику.</p>
 *
 * @param <T> тип сущности
 */
public abstract class AbstractJdbcRepository<T> {

    protected final DatabaseConnectionManager databaseConnectionManager;

    /**
     * Имя таблицы в БД.
     */
    protected abstract String getTableName();

    /**
     * Маппинг строки ResultSet в объект.
     * Свой для каждого типа сущности.
     */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    protected AbstractJdbcRepository(DatabaseConnectionManager connectionManager) {
        this.databaseConnectionManager = connectionManager;
    }

    public boolean existsById(UUID id) {
        String sql =  "SELECT 1 FROM " + getTableName() + " WHERE id = ?";

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new  RuntimeException("existsById error: " + e.getMessage(), e);
        }
    }

    public void deleteById(UUID id) {
        String sql =  "DELETE FROM " + getTableName() + " WHERE id = ?";

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteById error: " + e.getMessage(), e);
        }
    }

    public long count() {
        String sql =  "SELECT COUNT(*) FROM " + getTableName();

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new  RuntimeException("count error: " + e.getMessage(), e);
        }
    }

    public Optional<T> findById(UUID id) {
        String sql =  "SELECT * FROM " + getTableName() + " WHERE id = ?";

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if  (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new  RuntimeException("findById error: " + e.getMessage(), e);
        }
    }

    public List<T> findAll() {
        String sql =  "SELECT * FROM " + getTableName();
        List<T> list = new ArrayList<>();

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new  RuntimeException("findAll error: " + e.getMessage(), e);
        }
    }

    /**
     * Вспомогательный метод: выполнить SELECT и вернуть результат.
     * Позволяет избежать дублирования try-catch блоков в методах поиска.
     */
    protected List<T> executeQueryForList(String sql, PreparedStatementSetter setter) {
        List<T> list = new ArrayList<>();

        try (Connection connection = databaseConnectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            setter.setParameters(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new  RuntimeException("executeQueryForList error: " + e.getMessage(), e);
        }
    }

    /**
     * Вспомогательный функциональный интерфейс для установки параметров PreparedStatement.
     */
    @FunctionalInterface
    protected interface PreparedStatementSetter {
        void setParameters(PreparedStatement stmt) throws SQLException;
    }
}
