package com.paypocket.service;

import com.paypocket.model.Currency;
import com.paypocket.model.Transaction;
import com.paypocket.model.TransactionType;
import com.paypocket.model.User;
import com.paypocket.model.Wallet;
import com.paypocket.repository.TransactionRepository;
import com.paypocket.repository.UserRepository;
import com.paypocket.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService – юнит-тесты")
public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("getAllUsers: возвращает всех пользователей из репозитория")
    void getAllUsers_returnsAll() {
        User alice = new User("alice", "alice@mail.com", "hash");
        User bob = new User("bob", "bob@mail.com", "hash");
        when(userRepository.findAll()).thenReturn(List.of(alice, bob));

        List<User> users = adminService.getAllUsers();

        assertEquals(2, users.size());
        assertTrue(users.contains(alice));
        assertTrue(users.contains(bob));
    }

    @Test
    @DisplayName("getWalletsGroupedByUser: группирует кошельки по владельцу")
    void getWalletsGroupedByUser_groupsByOwner() {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        Wallet aliceRub = new Wallet(aliceId, "Основной", Currency.RUB);
        Wallet aliceUsd = new Wallet(aliceId, "Доллары", Currency.USD);
        Wallet bobRub = new Wallet(bobId, "Основной", Currency.RUB);
        when(walletRepository.findAll()).thenReturn(List.of(aliceRub, aliceUsd, bobRub));

        Map<UUID, List<Wallet>> grouped = adminService.getWalletsGroupedByUser();

        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get(aliceId).size());
        assertEquals(1, grouped.get(bobId).size());
        assertTrue(grouped.get(bobId).contains(bobRub));
    }

    @Test
    @DisplayName("getAllWallets: возвращает все кошельки системы")
    void getAllWallets_returnsAll() {
        Wallet w1 = new Wallet(UUID.randomUUID(), "Основной", Currency.RUB);
        Wallet w2 = new Wallet(UUID.randomUUID(), "Основной", Currency.EUR);
        when(walletRepository.findAll()).thenReturn(List.of(w1, w2));

        List<Wallet> wallets = adminService.getAllWallets();

        assertEquals(2, wallets.size());
    }

    @Test
    @DisplayName("getAllTransactions: пробрасывает пагинацию в репозиторий")
    void getAllTransactions_returnsPage() {
        Transaction tx = new Transaction.Builder(
                UUID.randomUUID(), TransactionType.DEPOSIT, new BigDecimal("100.00")).build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> page = new PageImpl<>(List.of(tx), pageable, 1);
        when(transactionRepository.findAll(pageable)).thenReturn(page);

        Page<Transaction> result = adminService.getAllTransactions(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(tx, result.getContent().get(0));
    }
}
