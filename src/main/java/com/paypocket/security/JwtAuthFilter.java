package com.paypocket.security;

import com.paypocket.model.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Фильтр, который проверяет JWT перед тем как пустить запрос в контроллер.
 *
 * <p>Срабатывает на каждый HTTP-запрос. Логика:
 * <ol>
 *   <li>Если путь не требует авторизации (логин, регистрация, swagger, web-страницы)
 *       — пропускаем без проверки.</li>
 *   <li>Иначе читаем заголовок Authorization. Ожидаем формат "Bearer &lt;token&gt;".</li>
 *   <li>Если заголовка нет или токен невалиден — возвращаем 401 с JSON-ошибкой.</li>
 *   <li>Если всё ок — кладём userId в атрибут запроса и передаём дальше по цепочке.</li>
 * </ol>
 *
 * <p>Контроллер потом достанет userId из атрибута через
 * {@code (UUID) request.getAttribute(JwtAuthFilter.USER_ID_ATTRIBUTE)}.</p>
 *
 * <p>OncePerRequestFilter — базовый класс из Spring, гарантирует, что
 * doFilterInternal вызовется ровно один раз на запрос (без него фильтр
 * мог бы сработать дважды на forward/include).</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Имя атрибута запроса, в который кладём userId — контроллеры читают его отсюда. */
    public static final String USER_ID_ATTRIBUTE = "authUserId";

    /** Имя атрибута запроса, в который кладём роль пользователя. */
    public static final String USER_ROLE_ATTRIBUTE = "authUserRole";

    /** Префикс путей, доступных только администраторам. */
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";

    /** Стандартная схема Bearer-токенов из RFC 6750. */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Запросы, которые НЕ требуют JWT.
     *
     * <p>Логин/регистрация — потому что у пользователя ещё нет токена.
     * Swagger — это документация, держим её открытой для удобства разработки.
     * Всё, что не начинается с /api/v1, относится к старому веб-интерфейсу
     * на Thymeleaf и работает через HTTP-сессию — JWT там не нужен.</p>
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1")
                || path.startsWith("/api/v1/auth/")
                || path.equals("/api/v1/users") && "POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "Отсутствует заголовок Authorization: Bearer <token>");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        if (!jwtService.isValid(token)) {
            writeUnauthorized(response, "Невалидный или просроченный токен");
            return;
        }

        UUID userId = jwtService.extractUserId(token);
        Role role = jwtService.extractRole(token);
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        request.setAttribute(USER_ROLE_ATTRIBUTE, role);

        // Ролевая проверка: эндпоинты /api/v1/admin/** доступны только администраторам.
        // Токен валиден (значит, 401 уже не нужен), но прав не хватает — отдаём 403.
        if (request.getRequestURI().startsWith(ADMIN_PATH_PREFIX) && role != Role.ADMIN) {
            writeForbidden(response, "Недостаточно прав: требуется роль ADMIN");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Возвращает 401 в формате JSON, совместимом с com.paypocket.dto.ErrorResponse —
     * чтобы клиенты обрабатывали ошибки авторизации так же, как остальные API-ошибки.
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    /**
     * Возвращает 403 в формате JSON: токен валиден, но роли не хватает.
     */
    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    private void writeError(HttpServletResponse response, HttpStatus status,
                            String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = "{\"code\":\"" + code + "\",\"message\":\""
                + escapeJson(message)
                + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
        response.getWriter().write(body);
    }

    /** Экранирует то немногое, что может встретиться в сообщении: кавычки и обратные слэши. */
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
