package com.parkease.booking_service.exception;

public class SpotUnavailableException extends RuntimeException {
    public SpotUnavailableException(String message) {
        super(message);
    }
}
