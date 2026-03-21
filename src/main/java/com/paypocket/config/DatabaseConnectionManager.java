package com.paypocket.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Управление соединениями с базой данных.
 *
 * <p>Сейчас создаёт новое соединение при каждом вызове getConnection().
 * На дне 9–10 заменим на пул соединений (HikariCP), который
 * переиспользует открытые соединения — значительно эффективнее.</p>
 */
public class DatabaseConnectionManager {

    private final String url;
    private final String username;
    private final String password;

    public DatabaseConnectionManager(AppConfig config) {
        url = config.getDbUrl();
        username = config.getDbUser();
        password = config.getDbPassword();
    }

    /**
     * Создает и возвращает новое соединение с БД.
     *
     * <p>ВАЖНО! Вызывающий код обязан закрыть соединение
     * через try-with-resources, иначе – утечка ресурсов.</p>
     *
     * @return новое соединение
     * @throws RuntimeException если подключиться не удалось
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось подключиться к БД: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет, доступна ли база данных.
     *
     * @return true если подключение успешно
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
}
