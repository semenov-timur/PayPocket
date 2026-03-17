package com.paypocket.repository;

import com.paypocket.model.User;
import com.paypocket.model.Wallet;

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

}
