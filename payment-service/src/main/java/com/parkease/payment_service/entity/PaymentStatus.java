package com.parkease.payment_service.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED;

    @JsonCreator
    public static PaymentStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }

        String normalized = value.trim().toUpperCase();
        if ("PAID".equals(normalized) || "COMPLETED".equals(normalized)) {
            return SUCCESS;
        }

        try {
            return PaymentStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return FAILED;
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
