package com.paypocket.repository;

import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий транзакций (операций).
 *
 * <p>Наследует JpaRepository, а значит Spring самостоятельно
 * сгенерирует реализацию всех методов по имени.</p>
 *
 * <p>Основные CRUD-методы уже есть в базовом репозитории,
 * здесь только специфичные запросы.</p>
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Находит все транзакции кошелька, отсортированные по дате (новые первые).
     *
     * @param walletId id кошелька
     * @return отсортированный список транзакций кошелька
     */
    List<Transaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    /**
     * Находит транзакции кошелька с пагинацией.
     *
     * <p>Pageble – объект Spring, содержащий номер страницы и размер.
     * Page – надстройка над списком с метаданными (всего страниц, всего записей).</p>
     *
     * <p>Вызов: repo.findByWalletId(walletId, PageRequest.of(0, 5, Sort.by("createdAt").descending())).</p>
     *
     * @param walletId id кошелька
     * @param pageable объект Spring, содержащий номер страницы и размер
     * @return страница транзакций
     */
    Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

    /**
     * Находит транзакции кошелька определенного типа.
     *
     * @param walletId  id кошелька
     * @param type      тип транзакции (фильтр)
     * @return отфильтрованный список транзакций кошелька
     */
    List<Transaction> findByWalletIdAndTypeOrderByCreatedAtDesc(UUID walletId, TransactionType type);
}
