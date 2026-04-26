package com.parkease.payment_service.mapper.Impl;

import com.parkease.payment_service.dtos.PaymentRequestDto;
import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRequestMapper implements Mapper<PaymentRequestDto, Payment> {
    private final ModelMapper mapper;

    @Override
    public PaymentRequestDto mapTo(Payment payment) {
        return mapper.map(payment, PaymentRequestDto.class);
    }

    @Override
    public Payment mapFrom(PaymentRequestDto paymentRequestDto) {
        return mapper.map(paymentRequestDto, Payment.class);
    }
}
