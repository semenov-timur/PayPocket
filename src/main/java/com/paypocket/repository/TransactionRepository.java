package com.paypocket.repository;

import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;

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
     * Находит транзакции кошелька определенного типа.
     *
     * @param walletId  id кошелька
     * @param type      тип транзакции (фильтр)
     * @return отфильтрованный список транзакций кошелька
     */
    List<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type);
}
