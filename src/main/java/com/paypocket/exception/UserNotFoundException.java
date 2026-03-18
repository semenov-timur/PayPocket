package com.paypocket.exception;

import java.util.UUID;

/**
 * Пользователь не найден.
 * Выбрасывается при попытке найти несуществующего пользователя.
 */
public class UserNotFoundException extends PayPocketException {

    private final String identifier;

    /**
     * @param identifier username или id пользователя (для сообщения об ошибке)
     */
    public UserNotFoundException(String identifier) {
        super("Пользователь не найден: " + identifier);
        this.identifier = identifier;
    }

    public UserNotFoundException(UUID id) {
        this(id.toString());
    }

    public String getIdentifier() {
        return identifier;
    }

}
