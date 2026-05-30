package com.paypocket.service;

import com.paypocket.model.Transaction;
import com.paypocket.model.User;
import com.paypocket.model.Wallet;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис администратора — только чтение данных всей системы.
 *
 * <p>Содержит операции, доступные исключительно роли ADMIN: просмотр всех
 * пользователей с их кошельками, всех кошельков и всех транзакций.
 * Сама проверка прав выполняется централизованно в
 * {@link com.paypocket.security.JwtAuthFilter} для путей {@code /api/v1/admin/**},
 * поэтому сюда запрос доходит уже от администратора.</p>
 *
 * <p>Все методы read-only — данные не изменяются, поэтому
 * {@code @Transactional(readOnly = true)} на уровне класса.</p>
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(UserRepository userRepository,
                        WalletRepository walletRepository,
                        TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Все пользователи системы.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Все кошельки, сгруппированные по идентификатору владельца.
     *
     * <p>Грузим кошельки одним запросом и группируем в памяти — это избавляет
     * от проблемы N+1 (отдельный запрос на кошельки каждого пользователя).</p>
     */
    public Map<UUID, List<Wallet>> getWalletsGroupedByUser() {
        return walletRepository.findAll().stream()
                .collect(Collectors.groupingBy(Wallet::getUserId));
    }

    /**
     * Все кошельки системы.
     */
    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    /**
     * Все транзакции системы с пагинацией.
     */
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }
}
