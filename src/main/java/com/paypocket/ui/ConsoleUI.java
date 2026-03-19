package com.paypocket.ui;

import com.paypocket.dto.TransferResult;
import com.paypocket.exception.PayPocketException;
import com.paypocket.model.Currency;
import com.paypocket.model.Transaction;
import com.paypocket.model.User;
import com.paypocket.model.Wallet;
import com.paypocket.service.UserService;
import com.paypocket.service.WalletService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    Scanner scanner;
    UserService userService;
    WalletService walletService;
    User currentUser;

    public ConsoleUI(UserService userService, WalletService walletService) {
        this.scanner = new Scanner(System.in);
        this.userService = userService;
        this.walletService = walletService;
    }

    // ===========================================
    // ГЛАВНЫЙ ЦИКЛ
    // ===========================================

    public void start() {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║     PayPocket – Digital Wallet     ║");
        System.out.println("╚════════════════════════════════════╝");

        boolean running = true;
        while (running) {
            if (currentUser == null) {
                running = showGuestMenu();
            }
            else {
                running = showUserMenu();
            }
        }

        System.out.println("Спасибо за использование PayPocket!");
        scanner.close();
    }

    // ===========================================
    // МЕНЮ ГОСТЯ (НЕ АВТОРИЗОВАН)
    // ===========================================

    private boolean showGuestMenu() {
        System.out.println("\n––– Главное меню –––");
        System.out.println("1. Войти");
        System.out.println("2. Зарегестрироваться");
        System.out.println("0. Выход");

        int choice = readInt("Выберите пункт: ");

        switch(choice) {
            case 1 -> handleLogin();
            case 2 -> handleRegister();
            case 0 -> { return false; }
            default -> System.out.println("Неизвестный пункт меню.");
        }

        return true;
    }

    // ===========================================
    // МЕНЮ ПОЛЬЗОВАТЕЛЯ (АВТОРИЗОВАН)
    // ===========================================

    private boolean showUserMenu() {
        System.out.println("\n––– Главное меню –––");
        System.out.println("1. Мои кошельки");
        System.out.println("2. Создать кошелек");
        System.out.println("3. Пополнить кошелек");
        System.out.println("4. Перевести средства");
        System.out.println("5. Снять деньги");
        System.out.println("6. История операций");
        System.out.println("7. Выйти из аккаунта");
        System.out.println("0. Выйти из приложения");

        int choice = readInt("Выберите пункт: ");

        switch(choice) {
            case 1 -> handleShowWallets();
            case 2 -> handleCreateWallet();
            case 3 -> handleDeposit();
            case 4 -> handleTransfer();
            case 5 -> handleWithdraw();
            case 6 -> handleShowHistory();
            case 7 -> handleLogout();
            case 0 -> { return false; }
        }

        return true;
    }

    // ===========================================
    // ОБРАБОТЧИКИ – АВТОРИЗАЦИЯ
    // ===========================================

    private void handleRegister() {
        System.out.println("\n––– Регистрация –––");

        String username = readString("Username: ");
        String email = readString("E-mail: ");
        String password = readString("Password: ");

        try {
            User user = userService.register(username, email, password);
            currentUser = user;
            System.out.println("Добро пожаловать, " + user.getUsername() + "!");
        } catch (PayPocketException e) {
            System.out.printf("Ошибка: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка валидации: " + e.getMessage());
        }
    }

    private void handleLogin() {
        System.out.println("\n––– Вход –––");

        String username = readString("Username: ");
        String password = readString("Password: ");

        try {
            User user = userService.authenticate(username, password);
            currentUser = user;
            System.out.println("Здравствуйте, " + currentUser.getUsername() + "!");
        } catch (PayPocketException e) {
            System.out.printf("Ошибка: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка аутентификации: " + e.getMessage());
        }
    }

    private void handleLogout() {
        System.out.println("До свидания, " +  currentUser.getUsername() + "!");
        currentUser = null;
    }

    // ===========================================
    // ОБРАБОТЧИКИ – КОШЕЛЬКИ
    // ===========================================

    private void handleShowWallets() {
        List<Wallet> wallets = walletService.getUserWallets(currentUser.getId());

        if  (wallets.isEmpty()) {
            System.out.println("\nУ Вас нет кошельков. Самое время создать первый.");
            return;
        }

        System.out.println("\n––– Ваши кошельки –––");
        System.out.printf("%-4s %-20s %15s %6s%n", "№", "Название", "Баланс", "Вылюта");
        System.out.println("–".repeat(47));
        for (int i = 1; i <= wallets.size(); i++) {
            Wallet wallet = wallets.get(i - 1);
            System.out.printf("%-4d %-20s %15s %6s%n",
                    i,
                    wallet.getName(),
                    wallet.getBalance(),
                    wallet.getCurrency()
            );
        }
    }

    private void handleCreateWallet() {
        System.out.println("\n––– Создание кошелька –––");

        String name = readString("Название кошелька: ");

        System.out.println("Доступные валюты:");
        for (Currency c : Currency.values()) {
            System.out.printf("  %s – %s (%s)%n",
                    c.name(),
                    c.getDescription(),
                    c.getSymbol()
            );
        }

        String currencyInput = readString("Валюта [по умолчанию RUB]: ");
        Currency currency;
        if (currencyInput.isBlank()) {
            currency = Currency.RUB;
        }
        else {
            currencyInput = currencyInput.toUpperCase();
            currency = Currency.fromString(currencyInput);
            if (currency == null) {
                System.out.println("Неизвестная валюта: " + currencyInput);
                return;
            }
        }

        try {
            Wallet wallet = walletService.createWallet(currentUser.getId(), name, currency);
            System.out.printf("Кошелек создан: %s (%s)%n",
                    wallet.getName(),
                    wallet.getCurrency()
            );
        } catch (PayPocketException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    // ===========================================
    // ОБРАБОТЧИКИ – ОПЕРАЦИИ
    // ===========================================

    private void handleDeposit() {
        System.out.println("\n––– Пополнение кошелька –––");

        Wallet wallet = selectWallet("Выберите кошелек для пополнения");
        if  (wallet == null) return;

        BigDecimal amount = readAmount("Сумма пополнения: ");
        if (amount == null) return;

        try {
            walletService.deposit(wallet.getId(), amount);
            BigDecimal newBalance = walletService.getBalance(wallet.getId());
            System.out.printf("Счет пополнен! Баланс: %s %s%n",
                    newBalance,
                    wallet.getCurrency()
            );
        } catch (PayPocketException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void handleTransfer() {
        System.out.println("\n––– Перевод средств –––");

        Wallet senderWallet = selectWallet("Выберите кошелек для списания");
        if   (senderWallet == null) return;

        System.out.printf("Текущий баланс: %s %s%n",
                senderWallet.getBalance(),
                senderWallet.getCurrency()
        );

        String recipientUsername = readString("Username получателя: ");

        User recipient;
        try {
            recipient = userService.getByUsername(recipientUsername);
        } catch (PayPocketException e) {
            System.out.println("Ошибка: " + e.getMessage());
            return;
        }

        List<Wallet> recipientWallets = walletService.getUserWallets(recipient.getId());
        if (recipientWallets.isEmpty()) {
            System.out.println("У получателя нет кошельков.");
            return;
        }

        List<Wallet> compatibleWallets = recipientWallets.stream()
                .filter(w -> w.getCurrency() == senderWallet.getCurrency())
                .toList();

        if (compatibleWallets.isEmpty()) {
            System.out.printf("У получателя нет кошельков в валюте %s.%n.",  senderWallet.getCurrency());
            return;
        }

        Wallet recieverWallet;
        if (compatibleWallets.size() == 1) {
            recieverWallet = compatibleWallets.getFirst();
            System.out.printf("Кошелек получателя: %s (%s)%n",
                    recieverWallet.getName(),
                    recieverWallet.getCurrency()
            );
        }
        else {
            System.out.println("Кошельки получателя:");
            for  (int i = 0; i < compatibleWallets.size(); i++) {
                Wallet wallet = compatibleWallets.get(i);
                System.out.printf("   %d. %s (%s)%n",
                        i + 1,
                        wallet.getName(),
                        wallet.getCurrency()
                );
            }

            int walletChoice = readInt("Номер кошелька: ") - 1;
            if  (walletChoice < 0 || walletChoice >= compatibleWallets.size()) {
                System.out.println("Некорректный выбор.");
                return;
            }

            recieverWallet = compatibleWallets.get(walletChoice);
        }

        BigDecimal amount = readAmount("Сумма перевода: ");
        if (amount == null) return;

        System.out.printf("Перевести %s %s пользователю %s? (да/нет): ",
                amount, senderWallet.getCurrency(), recipientUsername);
        String comfirm = readString("").toLowerCase();

        if (!comfirm.equals("да") && !comfirm.equals("yes") && !comfirm.equals("y")) {
            System.out.println("Перевод отменен.");
            return;
        }

        try {
            TransferResult result = walletService.transfer(senderWallet.getId(), recieverWallet.getId(), amount);
            System.out.println("Перевод выполнен!");
            System.out.printf("Ваш баланс: %s %s%n",
                    senderWallet.getBalance(),
                    senderWallet.getCurrency()
            );
        } catch (PayPocketException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void handleWithdraw() {
        System.out.println("\n––– Снятие средств –––");

        Wallet wallet = selectWallet("Выберите с какого кошелька снять");
        if  (wallet == null) return;

        BigDecimal amount = readAmount("Сумма для снятия: ");
        if (amount == null) return;

        try {
            walletService.withdraw(wallet.getId(), amount);
            BigDecimal newBalance = walletService.getBalance(wallet.getId());
            System.out.printf("Средства сняты! Баланс: %s %s%n",
                    newBalance,
                    wallet.getCurrency()
            );
        } catch (PayPocketException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void handleShowHistory() {
        System.out.println("\n––– Снятие средств –––");

        Wallet wallet = selectWallet("Выберите кошелек");
        if (wallet == null) return;

        List<Transaction> history = walletService.getTransactionHistory(wallet.getId());
        if (history.isEmpty()) {
            System.out.println("Операций пока нет.");
            return;
        }

        System.out.printf("%n%-4s %-16s %12s %-6s %-20s %s%n",
                "№", "Тип", "Сумма", "Валюта", "Дата", "Описание");
        System.out.println("–".repeat(75));

        for (int i  = 0; i < history.size(); i++) {
            Transaction transaction = history.get(i);
            System.out.printf("%-4d %-16s %12s %-6s %-20s %s%n",
                    i + 1,
                    transaction.getType(),
                    transaction.getAmount(),
                    wallet.getCurrency(),
                    transaction.getCreatedAt().format(DateTimeFormatter.ISO_DATE),
                    transaction.getDescription() != null ? transaction.getDescription() : ""
            );
        }

        System.out.printf("%nВсего операций: %d%n", history.size());
    }

    // ===========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ВВОДА
    // ===========================================

    private Wallet selectWallet(String prompt) {
        List<Wallet> wallets = walletService.getUserWallets(currentUser.getId());
        if (wallets.isEmpty()) {
            System.out.println("\nУ Вас нет кошельков. Самое время создать первый.");
            return null;
        }

        if (wallets.size() == 1) {
            Wallet wallet = wallets.getFirst();
            System.out.printf("Кошелек: %s (%s %s)%n",
                    wallet.getName(),
                    wallet.getBalance(),
                    wallet.getCurrency()
            );
            return wallet;
        }

        System.out.println(prompt + ":");
        for  (int i = 0; i < wallets.size(); i++) {
            Wallet wallet = wallets.get(i);
            System.out.printf("   %d. %s – %s %s%n",
                    i + 1,
                    wallet.getName(),
                    wallet.getBalance(),
                    wallet.getCurrency()
            );
        }

        int choice = readInt("Номер кошелька: ") - 1;
        if  (choice < 0 || choice >= wallets.size()) {
            System.out.println("Некорректный выбор.");
            return null;
        }

        return wallets.get(choice);
    }


    private String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private BigDecimal readAmount(String prompt) {
        System.out.print(prompt);
        try {
            BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
            if  (amount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Сумма должна быть больше нуля.");
                return null;
            }
            return amount;
        }  catch (NumberFormatException e) {
            System.out.println("Некорректная сумма.");
            return null;
        }
    }
}
