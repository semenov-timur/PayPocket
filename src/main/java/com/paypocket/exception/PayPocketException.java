package com.paypocket.exception;

/**
 * Базовое исключение приложения PayPocket.
 *
 * <p>Все остальные бизнес-исключения наследуются от этого класса.
 * Позволяет перехватить любую ошибку приложения одним блоком:
 * {@code catch (PayPocketException e)}, не ловя все RuntimeException подряд.</p>
 */
public class PayPocketException extends RuntimeException {

    public PayPocketException(String message) {
        super(message);
    }

    public PayPocketException(String message, Throwable cause) {
        super(message, cause);
    }
}
