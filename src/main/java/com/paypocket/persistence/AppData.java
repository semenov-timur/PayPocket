package com.paypocket.persistence;

import com.paypocket.model.Transaction;
import com.paypocket.model.User;
import com.paypocket.model.Wallet;

import java.util.ArrayList;
import java.util.List;

/**
 * Контейнер для всех данных приложения.
 * Используется для сериализации/десирализации в JSON.
 */
public class AppData {

    private List<User> users;
    private List<Wallet> wallets;
    private List<Transaction> transactions;

    public AppData() {
        this.users = new ArrayList<>();
        this.wallets = new ArrayList<>();
        this.transactions = new ArrayList<>();
    }

    public AppData(List<User> users, List<Wallet> wallets, List<Transaction> transactions) {
        this.users = users;
        this.wallets = wallets;
        this.transactions = transactions;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Wallet> getWallets() {
        return wallets;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
