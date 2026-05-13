package com.parkease.receipt_service.exception;

public class DuplicateReceiptException extends RuntimeException {
    public DuplicateReceiptException(String message) {
        super(message);
    }
}
