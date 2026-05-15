package com.parkease.booking_service.exception;

public class GracePeriodExpiredException extends RuntimeException {
    public GracePeriodExpiredException(String message) {
        super(message);
    }
}
