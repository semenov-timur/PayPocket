package com.paypocket.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Пользователь системы PayPocket.
 *
 * Каждый пользователь идентифицируется уникальным UUID.
 * Имя пользователя и e-mail также должны быть уникальными в системе –
 * однако это проверяется на сервиса/БД, а не в самой модели пользователя.
 */
public class User {
    private final UUID id;                  // final – идентификатор не изменяется после создания
    private String username;
    private String email;
    private String password;                // TODO: в дальнейшем хранить хэш
    private final LocalDateTime createdAt;

    /**
     * Конструктор для создания НОВОГО пользователя.
     * Генерирует случайной id и время создания.
     */
    public User(String username, String email, String password) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Конструктор для восстановления пользователя из хранилища.
     * (из файла или из БД), когда id уже существует.
     */
    public User(UUID id, String username, String email, String password, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.createdAt = createdAt;
    }

    // ––– Геттеры –––

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ––– Сеттеры – Для полей, которые могут меняться –––

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // ––– equals / hashCode по id –––
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(this.id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ––– toString для отладки –––

    @Override
    public String toString() {
        return "User{id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
