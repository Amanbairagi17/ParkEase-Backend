package com.parkease.booking_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFoundException(BookingNotFoundException exception) {
        log.error(exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
        errorResponse.setError("Booking not found");
        errorResponse.setMessage(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException exception) {
        log.error(exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Invalid booking state");
        errorResponse.setMessage(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({
            InvalidBookingStateException.class,
            GracePeriodExpiredException.class,
            UnauthorizedVehicleException.class,
            RefundNotAllowedException.class,
            PaymentFailedException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException exception) {
        log.error(exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Booking request rejected");
        errorResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler({SpotUnavailableException.class, BookingConflictException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException exception) {
        log.error(exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.CONFLICT.value());
        errorResponse.setError("Booking conflict");
        errorResponse.setMessage(exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        log.error(errors.toString());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Validation failed");
        errorResponse.setMessage(errors.toString());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        log.error(exception.getReason());

        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());
        errorResponse.setMessage(exception.getReason());

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler({
            TransactionSystemException.class,
            DataIntegrityViolationException.class,
            ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> handlePersistenceExceptions(RuntimeException exception) {
        Throwable root = getRootCause(exception);
        log.error("Persistence failure", root);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.CONFLICT.value());
        errorResponse.setError("Persistence conflict");
        errorResponse.setMessage(root.getMessage() != null ? root.getMessage() : "Database update failed");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException exception) {
        Throwable root = getRootCause(exception);
        log.error("Unhandled error", root);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimeStamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError("Internal server error");
        errorResponse.setMessage(root.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
