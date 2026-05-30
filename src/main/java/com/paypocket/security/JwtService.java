package com.paypocket.security;

import com.paypocket.model.Role;
import com.paypocket.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Сервис для генерации и проверки JWT-токенов.
 *
 * <p>JWT (JSON Web Token) — это просто строка из трёх частей через точку:
 * <pre>header.payload.signature</pre>
 *
 * <ul>
 *   <li><b>header</b>  — алгоритм подписи (например, HS256)</li>
 *   <li><b>payload</b> — данные (subject, expiration, наш userId/username)</li>
 *   <li><b>signature</b> — подпись секретом сервера. Гарантирует,
 *       что payload не был изменён клиентом.</li>
 * </ul>
 *
 * <p>Важно: payload НЕ зашифрован, только закодирован Base64.
 * Любой может прочитать его содержимое. Поэтому в токен НЕ кладём пароли
 * и приватные данные — только идентификаторы.</p>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Имя claim'а, в который кладём username (можно прочитать из токена). */
    private static final String CLAIM_USERNAME = "username";

    /** Имя claim'а с ролью пользователя — на нём строится проверка прав. */
    private static final String CLAIM_ROLE = "role";

    /** Секретный ключ HMAC. Им сервер подписывает токены и проверяет чужие подписи. */
    private final SecretKey secretKey;

    /** Сколько токен живёт (в миллисекундах). После — считается просроченным. */
    private final long ttlMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.ttl-ms}") long ttlMs
    ) {
        // Алгоритм HS256 требует минимум 256 бит (32 байта) ключа.
        // Если конфиг короче — JJWT сразу бросит исключение на старте, и это правильно:
        // лучше упасть при запуске, чем выдавать "слабые" токены в продакшене.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMs = ttlMs;
    }

    /**
     * Создаёт JWT для авторизованного пользователя.
     *
     * <p>Кладём в токен:
     * <ul>
     *   <li><b>subject</b> = userId — главный идентификатор</li>
     *   <li><b>username</b> — для удобства логов/UI</li>
     *   <li><b>role</b> — роль пользователя (USER/ADMIN) для проверки прав</li>
     *   <li><b>iat</b> (issued at) и <b>exp</b> (expiration) — JJWT их выставит сам</li>
     * </ul>
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Парсит токен и проверяет подпись + срок жизни.
     *
     * <p>Если что-то не так (подделка, истёк, мусор) — выбрасывает {@link JwtException}.
     * Возвращаем userId, который мы сами туда положили при генерации.</p>
     */
    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Извлекает роль из токена.
     *
     * <p>Если claim отсутствует или содержит неизвестное значение
     * (например, старый токен, выпущенный до внедрения ролей) — возвращаем
     * {@link Role#USER}. Так старые токены продолжают работать как обычные
     * пользователи, но не получают админских прав.</p>
     */
    public Role extractRole(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get(CLAIM_ROLE, String.class);
        if (role == null) {
            return Role.USER;
        }
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role claim in token: {} — defaulting to USER", role);
            return Role.USER;
        }
    }

    /**
     * Валидирует токен — true, если подпись корректна и срок не истёк.
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
