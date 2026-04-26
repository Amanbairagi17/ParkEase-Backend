package com.parkease.auth_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(UserNotFoundException.class)
    public  ResponseEntity<ErrorResponse> userNotFoundException(UserNotFoundException userNotFoundException){
        log.error(userNotFoundException.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setTimeStamp(LocalDateTime.now());
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setMessage("Group not found");
        error.setError(userNotFoundException.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponse> handeOtpException(OtpException exception){
        log.error(exception.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setTimeStamp(LocalDateTime.now());
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setMessage("Token not found");
        error.setError(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handelTokenNotFoundException(TokenNotFoundException ex) {
        log.error(ex.getMessage());

        ErrorResponse error = new ErrorResponse();
        error.setTimeStamp(LocalDateTime.now());
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setMessage("OTP max limit reached!!");
        error.setError(ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRunTimeException(RuntimeException exception){
        log.info(exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();

        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setMessage("Runtime Exception in application");
        errorResponse.setStatus(HttpStatus.NO_CONTENT.value());
        errorResponse.setError(exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentNotValidException(MethodArgumentNotValidException ex){

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        log.error(errors.toString());

        ErrorResponse error = new ErrorResponse();
        error.setError("Budget Application App");
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setMessage(errors.toString());
        error.setTimeStamp(LocalDateTime.now());

        return ResponseEntity.badRequest().body(error);
    }
}
