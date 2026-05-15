package com.parkease.booking_service.exception;

public class RefundNotAllowedException extends RuntimeException {
    public RefundNotAllowedException(String message) {
        super(message);
    }
}
