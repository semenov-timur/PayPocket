package com.paypocket.controller.api;

import com.paypocket.dto.CreateUserRequest;
import com.paypocket.dto.LoginResponse;
import com.paypocket.dto.UserResponse;
import com.paypocket.model.User;
import com.paypocket.security.JwtService;
import com.paypocket.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final JwtService jwtService;

    public UserApiController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/v1/users — регистрация.
     *
     * <p>@Valid — Spring проверяет аннотации валидации на CreateUserRequest
     * (@NotBlank, @Email, @Size). Если невалидно — вернёт 400 автоматически.</p>
     *
     * <p>@RequestBody — Spring парсит JSON из тела запроса в объект.</p>
     *
     * <p>В ответ кладём JWT-токен — пользователь оказывается сразу залогинен
     * и может дёргать защищённые эндпоинты без отдельного /auth/login.</p>
     */
    @PostMapping
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );
        String token = jwtService.generateToken(user);
        return ResponseEntity
                .status(HttpStatus.CREATED)     // 201
                .body(new LoginResponse(token, user.getId(), user.getUsername()));
    }

    /**
     * GET /api/v1/users/{id} — получение пользователя по id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
