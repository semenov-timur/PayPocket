package com.paypocket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для регистрации пользователя.
 * Аннотации для валидации проверяются автоматически при @Valid.
 */
public class CreateUserRequest {

    @NotBlank(message = "Username обязателен")
    @Size(min = 3, max = 50, message = "Username от 3 до 50 символов")
    private String username;

    @NotBlank(message = "E-mail обязателен")
    @Email(message = "Некорректный формат e-mail")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 4, message = "Пароль минимум 4 символа")
    private String password;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
