package com.paypocket.controller.api;

import com.paypocket.dto.AdminTransactionResponse;
import com.paypocket.dto.AdminUserResponse;
import com.paypocket.dto.WalletResponse;
import com.paypocket.model.Wallet;
import com.paypocket.service.AdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API администратора.
 *
 * <p>Все пути начинаются с {@code /api/v1/admin} — именно по этому префиксу
 * {@link com.paypocket.security.JwtAuthFilter} пускает только пользователей
 * с ролью ADMIN (иначе 403). Поэтому здесь дополнительная проверка прав
 * не нужна — до контроллера доходит только администратор.</p>
 *
 * <p>Эндпоинты:
 * <ul>
 *   <li>{@code GET /api/v1/admin/users} — все пользователи с их кошельками</li>
 *   <li>{@code GET /api/v1/admin/wallets} — все кошельки системы</li>
 *   <li>{@code GET /api/v1/admin/transactions} — все транзакции (постранично)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {

    private final AdminService adminService;

    public AdminApiController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * GET /api/v1/admin/users — все пользователи вместе с их кошельками.
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        Map<UUID, List<Wallet>> walletsByUser = adminService.getWalletsGroupedByUser();
        List<AdminUserResponse> users = adminService.getAllUsers().stream()
                .map(user -> AdminUserResponse.of(
                        user,
                        walletsByUser.getOrDefault(user.getId(), List.of())))
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/v1/admin/wallets — все кошельки системы.
     */
    @GetMapping("/wallets")
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        List<WalletResponse> wallets = adminService.getAllWallets().stream()
                .map(WalletResponse::from)
                .toList();
        return ResponseEntity.ok(wallets);
    }

    /**
     * GET /api/v1/admin/transactions — все транзакции системы, постранично
     * (новые первыми).
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<AdminTransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Pageable pageable = PageRequest.of(
                pageNumber, pageSize, Sort.by("createdAt").descending());
        List<AdminTransactionResponse> transactions = adminService
                .getAllTransactions(pageable)
                .map(AdminTransactionResponse::from)
                .getContent();
        return ResponseEntity.ok(transactions);
    }
}
