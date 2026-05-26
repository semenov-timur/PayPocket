package com.paypocket.dto;

import java.util.UUID;

/**
 * Ответ на успешный логин.
 *
 * <p>Клиент должен сохранить {@code token} и отправлять его в заголовке
 * {@code Authorization: Bearer <token>} в каждом последующем запросе к /api/v1/*.</p>
 */
public class LoginResponse {

    private final String token;
    private final UUID userId;
    private final String username;

    public LoginResponse(String token, UUID userId, String username) {
        this.token = token;
        this.userId = userId;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
