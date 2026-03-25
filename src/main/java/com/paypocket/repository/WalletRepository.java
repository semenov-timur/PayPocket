package com.paypocket.repository;

import com.paypocket.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий кошельков.
 *
 * <p>Расширяет базовый {@link Repository} методом поиска
 * кошельков по id владельца.</p>
 */
public interface WalletRepository extends Repository<Wallet, UUID> {

    /**
     * Находит кошельки по id владельца.
     *
     * @param userId идентификатор владельца
     * @return список кошельков (может быть пустым)
     */
    List<Wallet> findByUserId(UUID userId);

    /**
     * Находит кошелёк с блокировкой строки (SELECT FOR UPDATE).
     * Используется внутри транзакции для предотвращения
     * конкурентных изменений баланса.
     *
     * @param conn      соединение с активной транзакцией
     * @param walletId  id кошелька
     * @return          кошелёк или пустой Optional
     */
    Optional<Wallet> findByIdForUpdate(Connection conn, UUID walletId) throws SQLException;

    /**
     * Обновляет баланс кошелька в рамках существующей транзакции.
     *
     * @param conn          соединение с активной транзакцией
     * @param walletId      id кошелька
     * @param newBalance    новый баланс
     */
    public void updateBalance(Connection conn, UUID walletId, BigDecimal newBalance) throws SQLException;
}
