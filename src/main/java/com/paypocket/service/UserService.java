package com.paypocket.service;

import com.paypocket.exception.DuplicateUserException;
import com.paypocket.exception.UserNotFoundException;
import com.paypocket.model.User;
import com.paypocket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Сервис управления пользователями.
 *
 * <p>Отвечает за регистрацию, поиск и валидацию пользователей.
 * Все бизнес-правила проверяются здесь.</p>
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * PasswordEncoder — интерфейс для шифрования пароля.
     * Реализация BCryptPasswordEncoder:
     *   - encode("1234") → "$2a$10$abcdef..." (длинная строка-хэш)
     *   - matches("1234", "$2a$10$abcdef...") → true / false
     * Хэш необратим: из хэша нельзя получить исходный пароль.
     * Каждый вызов encode даёт РАЗНЫЙ хэш (внутри генерируется случайная "соль"),
     * но matches всё равно правильно сравнивает.
     */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Регистрирует нового пользователя.
     * Пароль хэшируется перед сохранением — в БД сырого пароля никогда нет.
     */
    @Transactional
    public User register(String username, String email, String password) {
        validateRegistrationInput(username, email, password);

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            log.warn("Registration taken – username taken: {}", username);
            throw new DuplicateUserException("username", username);
        } else if (userRepository.existsByEmail(email)) {
            log.warn("Registration taken – email taken: {}", email);
            throw new DuplicateUserException("email", email);
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username, email, hashedPassword);
        userRepository.save(user);

        log.info("User registered successfully: username – {}, id – {}", username, user.getId());
        return user;
    }

    /**
     * Аутентификация: ищем пользователя и сверяем введённый пароль с сохранённым хэшем.
     *
     * <p>passwordEncoder.matches(raw, hash) сам хэширует raw с той же солью, что внутри hash,
     * и сравнивает результаты. Сравнивать строки напрямую через equals нельзя —
     * в БД лежит хэш, а пользователь присылает сырой пароль.</p>
     */
    public User authenticate(String username, String password) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> {
                    log.warn("Auth failed – user not found: {}", username);
                    return new UserNotFoundException(username);
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Auth failed – wrong password: username – {}", username);
            throw new IllegalArgumentException("Неверный пароль!");
        }

        log.info("User authenticated: username = {}", username);
        return user;
    }

    public User getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private void validateRegistrationInput(String username, String email, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail не может быть пустым");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Некорректный e-mail: " + email);
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 4 символа");
        }
    }
}
