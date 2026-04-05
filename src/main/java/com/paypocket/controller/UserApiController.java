package com.paypocket.controller;

import com.paypocket.dto.CreateUserRequest;
import com.paypocket.dto.UserResponse;
import com.paypocket.model.User;
import com.paypocket.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API для пользователей.
 *
 * <p>@RestController — каждый метод возвращает объект,
 * Spring автоматически конвертирует его в JSON.</p>
 *
 * <p>@RequestMapping("/api/v1/users") — общий префикс для всех методов.</p>
 *
 * <p>ResponseEntity — обёртка, позволяющая задать HTTP-статус:
 * ResponseEntity.ok(body) = 200 + тело
 * ResponseEntity.status(201).body(body) = 201 + тело
 * ResponseEntity.notFound().build() = 404 без тела</p>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/v1/users — регистрация.
     *
     * <p>@Valid — Spring проверяет аннотации валидации на CreateUserRequest
     * (@NotBlank, @Email, @Size). Если невалидно — вернёт 400 автоматически.</p>
     *
     * <p>@RequestBody — Spring парсит JSON из тела запроса в объект.</p>
     */
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)     // 201
                .body(UserResponse.from(user));
    }

    /**
     * POST /api/v1/users/{id} — получение пользователя по id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * GET /api/v1/users — получение списка всех пользователей.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
