package org.example.Handler;

import java.util.Map;

import org.example.exception.DuplicateUsernameException;
import org.example.exception.InvalidCredentialsException;
import org.example.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
            .collect(java.util.stream.Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() == null ? "valor inválido" : error.getDefaultMessage(),
                (a, b) -> a
            ));
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "errors", fields));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Map<String, String>> invalid(InvalidCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Credenciales inválidas");
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(UserNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    ResponseEntity<Map<String, String>> duplicate(DuplicateUsernameException exception) {
        return error(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpected(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrió un error inesperado");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}
