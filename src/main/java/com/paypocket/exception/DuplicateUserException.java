package com.paypocket.exception;

/**
 * Пользователь уже существует.
 * Выбрасывается при попытке создать пользователя с уже занятым username или email.
 */
public class DuplicateUserException extends PayPocketException {

    private final String field;
    private final String value;

    public DuplicateUserException(String field, String value) {
        super(String.format("%s уже используется: %s", field, value));
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return  value;
    }
}
