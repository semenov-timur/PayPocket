package com.paypocket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Пользователь системы PayPocket.
 * <p>
 * Каждый пользователь идентифицируется уникальным UUID.
 * Имя пользователя и e-mail также должны быть уникальными в системе –
 * однако это проверяется на сервиса/БД, а не в самой модели пользователя.
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id")
    private UUID id;                  // final – идентификатор не изменяется после создания

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 50)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;                // хранится BCrypt-хэш (см. UserService)

    /**
     * Роль пользователя. По умолчанию USER — новый пользователь не может
     * стать администратором при регистрации. Хранится строкой ('USER'/'ADMIN').
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Конструктор без аргументов – для JPA / Hibernate.
     */
    protected User() {
    }

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

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
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
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}
