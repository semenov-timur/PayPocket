package com.paypocket.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Чтение конфигураций приложения из application.properties.
 *
 * <p>Properties – стандартный Java-класс для работы с файлами
 * формата ключ-значение. Файл загружается из classpath
 * (папка resources/). Classpath — это набор путей, где JVM ищет
 * классы и ресурсы. Папка src/main/resources/ автоматически
 * попадает в classpath при сборке Gradle-проекта.</p>
 */
public class AppConfig {

    private final Properties properties;

    public AppConfig() {
        properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input == null) {
                throw new RuntimeException(
                        "Файл application.properties не найден в resources/"
                );
            }

            properties.load(input);
        } catch (IOException e) {
            throw new  RuntimeException("Ошибка чтения application.properties", e);
        }
    }

    public String getDbUrl() {
        return properties.getProperty("db.url");
    }

    public String getDbUser() {
        return properties.getProperty("db.user");
    }
    public String getDbPassword() {
        return properties.getProperty("db.password");
    }
}
