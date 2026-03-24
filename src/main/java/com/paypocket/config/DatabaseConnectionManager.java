package com.paypocket.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Управление соединениями через пул HikariCP.
 *
 * <p>HikariDataSource при создании открывает несколько TCP-соединений
 * с PostgreSQL и держит их открытыми. При вызове getConnection()
 * возвращается уже готовое соединение из пула — мгновенно.</p>
 *
 * <p>Когда код вызывает connection.close() внутри try-with-resources,
 * соединение НЕ закрывается по-настоящему — оно возвращается в пул
 * для повторного использования. HikariCP подменяет поведение close().</p>
 */
public class DatabaseConnectionManager {

    private final HikariDataSource dataSource;

    public DatabaseConnectionManager(AppConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(config.getDbUrl());
        hikariConfig.setUsername(config.getDbUser());
        hikariConfig.setPassword(config.getDbPassword());

        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(5000);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setPoolName("PayPocketPool");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Получает соединение из пула.
     *
     * <p>Еслисоединение открыто – операция мгновенна.
     * При вызове close() соединение просто вернется в пул,
     * а не закроется</p>
     *
     * @return соединение из пула
     * @throws RuntimeException если не удалось получить соединение
     */
    public Connection getConnection() {
        try {
            this.dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось получить соединение из пула: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет, доступна ли база данных.
     *
     * @return true если подключение успешно
     */
    public boolean testConnection() {
        try (Connection conn = this.getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Закрывает пул соединений. Вызвать при завершении приложения.
     *
     * <p>Закроет все TCP-соединения с PostgreSQL.
     * После вызова getConnection() не будет работать.</p>
     */
    public void close() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
