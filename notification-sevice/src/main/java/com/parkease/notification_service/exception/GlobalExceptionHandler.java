package com.parkease.notification_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setTimeStamp(LocalDateTime.now());
        error.setStatus(HttpStatus.FORBIDDEN.value());
        error.setMessage("Access Denied");
        error.setError(ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setTimeStamp(LocalDateTime.now());
        error.setStatus(HttpStatus.UNAUTHORIZED.value());
        error.setMessage("Invalid or missing authentication");
        error.setError(ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> handleNumberFormatException(NumberFormatException ex) {
        log.error("Invalid number format: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setTimeStamp(LocalDateTime.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setMessage("Invalid userId format");
        error.setError(ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("Method argument type mismatch: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setTimeStamp(LocalDateTime.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setMessage("Invalid userId format");
        error.setError(ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setTimeStamp(LocalDateTime.now());
        error.setStatus(HttpStatus.UNAUTHORIZED.value());
        error.setMessage("Authentication required");
        error.setError(ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRunTimeException(RuntimeException exception) {
        log.info(exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();

        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setMessage("Runtime Exception in application");
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


}
