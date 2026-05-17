package com.paypocket.controller.api;

import com.paypocket.dto.LoginRequest;
import com.paypocket.dto.LoginResponse;
import com.paypocket.model.User;
import com.paypocket.security.JwtService;
import com.paypocket.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API авторизации.
 *
 * <p>Единственная задача — обменять пару (username, password) на JWT-токен.
 * Регистрация осталась на {@code POST /api/v1/users} (UserApiController).
 *
 * <p>Все остальные защищённые эндпоинты ждут заголовок
 * {@code Authorization: Bearer <token>}, который выдаёт этот контроллер.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthApiController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/v1/auth/login — обмен логина/пароля на JWT.
     *
     * <p>Если пара (username, password) валидна — возвращаем токен и базовую инфу о юзере.
     * Если нет — UserService бросает исключение, которое подхватывает GlobalExceptionHandler
     * и превращает в 404/400 с JSON-ошибкой.</p>
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.getUsername(), request.getPassword());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getUsername()));
    }
}
