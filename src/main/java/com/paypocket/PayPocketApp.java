package com.paypocket;

import com.paypocket.config.AppConfig;
import com.paypocket.config.DatabaseConnectionManager;
import com.paypocket.persistence.JsonDataPersistence;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;
import com.paypocket.repository.inmemory.InMemoryTransactionRepository;
import com.paypocket.repository.inmemory.InMemoryUserRepository;
import com.paypocket.repository.inmemory.InMemoryWalletRepository;
import com.paypocket.repository.jdbc.JdbcTransactionRepository;
import com.paypocket.repository.jdbc.JdbcUserRepository;
import com.paypocket.repository.jdbc.JdbcWalletRepository;
import com.paypocket.service.UserService;
import com.paypocket.service.WalletService;
import com.paypocket.ui.ConsoleUI;

import java.nio.file.Path;

/**
 * Точка входа в приложние PayPocket.
 *
 * <p>Собирает зависимоти (DI) вручную
 * и запускает консольный интерфейс.</p>
 */
public class PayPocketApp {

    private static final String DATA_FILE = "data/paypocket.json";

    public static void main(String[] args) {

        // Подключение к БД
        AppConfig config = new AppConfig();
        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(config);

        if (!connectionManager.testConnection()) {
            System.out.println("Не удалось подключиться к PostgreSQL");
        }
        System.out.println("Подключение к PostgreSQL успешно!");

        // Репозитории с БД
        UserRepository userRepository = new JdbcUserRepository(connectionManager);
        WalletRepository walletRepository = new JdbcWalletRepository(connectionManager);
        TransactionRepository transactionRepository = new JdbcTransactionRepository(connectionManager);

        UserService userService = new UserService(userRepository);
        WalletService walletService = new WalletService(walletRepository, transactionRepository);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nЗакрытие соединений с БД...");
            connectionManager.close();
        }));

        ConsoleUI ui = new ConsoleUI(userService, walletService);
        ui.start();
    }
}
