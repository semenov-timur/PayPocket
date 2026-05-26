package com.paypocket.controller.api;

import com.paypocket.dto.ErrorResponse;
import com.paypocket.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Глобальный обработчик ошибок для REST API.
 *
 * <p>{@code @RestControllerAdvice} — Spring перехватывает исключения из ВСЕХ
 * {@code @RestController} и направляет в соответствующий {@code @ExceptionHandler}.
 * Клиент всегда получает структурированный JSON с кодом ошибки и сообщением.
 */
@RestControllerAdvice(basePackages = "com.paypocket.controller.api")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 404 – ресурс не найден.
     */
    @ExceptionHandler({UserNotFoundException.class, WalletNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(PayPocketException e) {
        log.warn("Not found: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }

    /**
     * 409 – конфликт (дубликат).
     */
    @ExceptionHandler({DuplicateUserException.class, WalletAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleConflict(PayPocketException e) {
        log.warn("Conflict: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", e.getMessage()));
    }

    /**
     * 400 – бизнес-ошибки.
     */
    @ExceptionHandler({
            InsufficientFundsException.class,
            InvalidAmountException.class,
            SelfTransferException.class,
            CurrencyMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(PayPocketException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    /**
     * 403 – операция запрещена (нет прав на ресурс).
     */
    @ExceptionHandler(WalletOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(PayPocketException e) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", e.getMessage()));
    }

    /**
     * 400 – ошибки валидации (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation error: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    /**
     * 400 – общие ошибки аргументов.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_ARGUMENT", e.getMessage()));
    }

    /**
     * 500 — непредвиденная ошибка.
     * Последний рубеж — ловит всё, что не поймали обработчики выше.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "Внутренняя ошибка сервера"));
    }
}
