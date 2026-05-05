package com.parkease.payment_service.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus status) {
        return status == null ? PaymentStatus.PENDING.name() : status.name();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String value) {
        return PaymentStatus.fromValue(value);
    }
}
