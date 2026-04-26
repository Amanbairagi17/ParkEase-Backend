package com.parkease.payment_service.mapper.Impl;

import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentResponseMapper implements Mapper<PaymentResponseDto, Payment> {
    private final ModelMapper mapper;

    @Override
    public PaymentResponseDto mapTo(Payment payment) {
        return mapper.map(payment, PaymentResponseDto.class);
    }

    @Override
    public Payment mapFrom(PaymentResponseDto paymentResponseDto) {
        return mapper.map(paymentResponseDto, Payment.class);
    }
}
