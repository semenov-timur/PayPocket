package com.paypocket.service;

import com.paypocket.exception.DuplicateUserException;
import com.paypocket.exception.UserNotFoundException;
import com.paypocket.model.User;
import com.paypocket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Сервис управления пользователями.
 *
 * <p>Отвечает за регистрацию, поиск и валидацию пользователей.
 * Все бизнес-правила проверяются здесь.</p>
 */
public class UserService {

    private final UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /**
     * Конструктор с внедрением зависимости (DI - dependency injection).
     *
     * <p>Принимает интерфейс UserRepository и не привязан к контретной реализации.
     * Можно передать InMemoryUserRepository, JdbcUserRepository или мок для тестов.</p>
     *
     * @param userRepository репозиторий пользователей
     */
    public  UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Регистрирует нового пользователя.
     *
     * <p>Проверяет корректность и уникальность
     * username и email перед созданием.</p>
     *
     * @param username  имя пользователя
     * @param email     электронная почта
     * @param password  пароль
     * @return созданный пользователь
     * @throws DuplicateUserException если username или email уже заняты
     */
    public User register(String username, String email, String password) {
        // Валидация входных данных
        validateRegistrationInput(username, email, password);

        // Проверка уникальности
        if (userRepository.existsByUsername(username)) {
            log.warn("Registration taken – username taken: {}", username);
            throw new DuplicateUserException("username",  username);
        }
        else if (userRepository.existsByEmail(email)) {
            log.warn("Registration taken – email taken: {}", email);
            throw new DuplicateUserException("email",  email);
        }

        // Создание и сохранение
        // TODO: хэширование паролей
        User user = new User(username, email, password);
        userRepository.save(user);
        log.info("User registered successfully: username – {}, id – {}", username, user.getId());
        return user;
    }

    /**
     * Аутентификация пользователя.
     *
     * @param username имя пользователя
     * @param password пароль
     * @return авторизованный пользователь
     * @throws UserNotFoundException если пользователь не найден
     * @throws IllegalArgumentException если пароль неверный
     */
    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Auth failed – user not found: {}", username);
                    return new UserNotFoundException(username);
                } );

        if  (!user.getPassword().equals(password)) {
            log.warn("Auth failed – wrong password: username – {}", username);
            throw new IllegalArgumentException("Неверный пароль!");
        }

        return user;
    }

    /**
     * Находит пользователя по username.
     *
     * @param username имя пользователя
     * @return найденный пользователь
     * @throws UserNotFoundException если пользователь не найден
     */
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    /**
     * Находит пользователя по id.
     *
     * @param userId имя пользователя
     * @return найденный пользователь
     * @throws UserNotFoundException если пользователь не найден
     */
    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Возвращает список всех пользователей.
     *
     * @return список пользователей (может быть пустым)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // TODO: добавить расширенные проверки

    /**
     * Проверяет корректность входных данных при регистрации.
     * Здесь пока базовые проверки. Нужно использовать Bean validation.
     */
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
