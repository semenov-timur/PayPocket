package com.paypocket.dto;

import com.paypocket.model.User;
import com.paypocket.model.Wallet;

import java.util.List;
import java.util.UUID;

/**
 * DTO для администратора: пользователь вместе с его кошельками.
 *
 * <p>Используется на эндпоинте {@code GET /api/v1/admin/users}, чтобы
 * администратор за один запрос видел и пользователей, и их кошельки.</p>
 */
public class AdminUserResponse {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private String createdAt;
    private List<WalletResponse> wallets;

    /**
     * Фабричный метод. Собирает пользователя и его кошельки в один объект.
     */
    public static AdminUserResponse of(User user, List<Wallet> wallets) {
        AdminUserResponse response = new AdminUserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.role = user.getRole().name();
        response.createdAt = user.getCreatedAt().toString();
        response.wallets = wallets.stream()
                .map(WalletResponse::from)
                .toList();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<WalletResponse> getWallets() {
        return wallets;
    }
}
