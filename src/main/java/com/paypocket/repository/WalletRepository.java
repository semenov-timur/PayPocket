package com.paypocket.repository;

import com.paypocket.model.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий кошельков.
 *
 * <p>Наследует JpaRepository, а значит Spring самостоятельно
 * сгенерирует реализацию всех методов по имени.</p>
 *
 * <p>Основные CRUD-методы уже есть в базовом репозитории,
 * здесь только специфичные запросы.</p>
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Находит кошельки по id владельца.
     *
     * @param userId идентификатор владельца
     * @return список кошельков (может быть пустым)
     */
    List<Wallet> findByUserIdOrderByCreatedAtAsc(UUID userId);

    /**
     * Находит кошелёк с блокировкой строки (SELECT FOR UPDATE).
     * Используется внутри транзакции для предотвращения
     * конкурентных изменений баланса.
     *
     * @param walletId id кошелька
     * @return кошелёк или пустой Optional
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID walletId);
}
