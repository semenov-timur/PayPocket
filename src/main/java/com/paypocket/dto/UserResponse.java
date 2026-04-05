package com.paypocket.dto;

import com.paypocket.model.User;

import java.util.UUID;

/**
 * DTO для получения пользователя.
 */
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String createdAt;

    /**
     * Фабричный метод.
     * Конвертирует User в безопасный для API объект.
     */
    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.createdAt = user.getCreatedAt().toString();
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
    public String getCreatedAt() {
        return createdAt;
    }
}
