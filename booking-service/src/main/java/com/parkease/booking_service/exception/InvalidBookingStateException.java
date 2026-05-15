package com.parkease.booking_service.exception;

public class InvalidBookingStateException extends RuntimeException {
    public InvalidBookingStateException(String message) {
        super(message);
    }
}
