package com.paypocket.persistence;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Сохранение и загрузка данных в/из JSON-файла.
 * Временное решение до внедрения БД.
 */
public class JsonDataPersistence {

    private final Path filePath;
    private final Gson gson;

    public  JsonDataPersistence(Path filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .serializeNulls()
                .setPrettyPrinting()
                .create();
    }

    public void load(UserRepository userRepo, WalletRepository walletRepo, TransactionRepository transactionRepo) {
        if (!Files.exists(filePath)) {
            System.out.println("Файл данных не найден. Начинаем с чистого состояния.");
            return;
        }

        try {
            String json = Files.readString(filePath);
            AppData data = gson.fromJson(json, AppData.class);

            if  (data == null) {
                System.out.println("Файл данных пуст. Начинаем с чистого состояния.");
                return;
            }

            if (data.getUsers() != null) {
                data.getUsers().forEach(userRepo::save);
            }
            if (data.getWallets() != null) {
                data.getWallets().forEach(walletRepo::save);
            }
            if (data.getTransactions() != null) {
                data.getTransactions().forEach(transactionRepo::save);
            }

            System.out.printf("Данные загружены: %d пользователей, %d кошельков, %d операций%n",
                    data.getUsers() != null ? data.getUsers().size() : 0,
                    data.getWallets() != null ? data.getWallets().size() : 0,
                    data.getTransactions() != null ? data.getTransactions().size() : 0
            );
        } catch (IOException e) {
            System.out.println("Ошибка чтения данных из файла: " + e.getMessage());
            System.out.println("Начинаем с чистого состояния.");
        }
    }

    public void save(UserRepository userRepo, WalletRepository walletRepo, TransactionRepository transactionRepo) {
        AppData data = new AppData(
                userRepo.findAll(),
                walletRepo.findAll(),
                transactionRepo.findAll()
        );

        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }

            String json  = gson.toJson(data);
            Files.writeString(filePath, json);

            System.out.printf("Данные сохранены: %d пользователей, %d кошельков, %d транзакций%n",
                    data.getUsers().size(),
                    data.getWallets().size(),
                    data.getTransactions().size()
            );
        } catch (IOException e) {
            System.out.println("Ошибка сохранения данных: " + e.getMessage());
        }
    }

}
