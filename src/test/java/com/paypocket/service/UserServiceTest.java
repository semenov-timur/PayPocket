package com.paypocket.service;

import com.paypocket.exception.DuplicateUserException;
import com.paypocket.exception.UserNotFoundException;
import com.paypocket.model.User;
import com.paypocket.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService – юнит-тесты")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Регистрация: успешная регистрация нового пользователя")
    void register_shouldCreateUser() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User user = userService.register("alice", "alice@mail.com", "1234");

        assertNotNull(user);
        assertEquals("alice", user.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация: дубликат username — DuplicateUserException")
    void register_duplicateUsername_shouldThrow() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () ->
                userService.register("alice", "alice@mail.com", "1234")
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Аутентификация: неверный пароль — исключение")
    void authenticate_wrongPassword_shouldThrow() {
        User user = new User("alice", "alice.mail.com", "1234");
        when(userRepository.findByUsernameIgnoreCase("alice"))
                .thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () ->
                userService.authenticate("alice", "wrongpassword")
        );
    }

    @Test
    @DisplayName("Поиск: несуществующий пользователь — UserNotFoundException")
    void getByUsername_notFound_shouldThrow() {
        when(userRepository.findByUsernameIgnoreCase("ghost"))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userService.getByUsername("ghost")
        );
    }
}
