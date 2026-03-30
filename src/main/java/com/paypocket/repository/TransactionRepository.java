package com.paypocket.repository;

import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий транзакций (операций).
 * <p>Расширяет базовый {@link Repository} методом поиска
 * транзакций по id кошелька или владельца.</p>
 */
public interface TransactionRepository extends Repository<Transaction, UUID> {

    /**
     * Находит все транзакции кошелька, отсортированные по дате (новые первые).
     *
     * @param walletId id кошелька
     * @return отсортированный список транзакций кошелька
     */
    List<Transaction> findByWalletId(UUID walletId);

    /**
     * Находит транзакции кошелька с пагинацией.
     *
     * @param walletId      id кошелька
     * @param pageNumber    номер страницы начиная с 1
     * @param pageSize      кол-во записей на странице
     * @return  страница транзакций
     */
    List<Transaction> findByWalletId(UUID walletId, int pageNumber, int pageSize);

    /**
     * Находит транзакции кошелька определенного типа.
     *
     * @param walletId  id кошелька
     * @param type      тип транзакции (фильтр)
     * @return отфильтрованный список транзакций кошелька
     */
    List<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type);

    /**
     * Сохраняет транзакцию в рамках существующего соединения/транзакции.
     *
     * @param conn        соединение с активной транзакцией
     * @param transaction транзакция для сохранения
     */
    public void save(Connection conn, Transaction transaction) throws SQLException;
}
