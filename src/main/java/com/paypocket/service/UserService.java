package com.paypocket.service;

import com.paypocket.exception.DuplicateUserException;
import com.paypocket.exception.UserNotFoundException;
import com.paypocket.model.User;
import com.paypocket.repository.UserRepository;

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
            throw new DuplicateUserException("username",  username);
        }
        else if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserException("email",  email);
        }

        // Создание и сохранение
        // TODO: хэширование паролей
        User user = new User(username, email, password);
        return userRepository.save(user);
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
