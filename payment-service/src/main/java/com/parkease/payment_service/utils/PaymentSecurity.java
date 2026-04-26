package com.parkease.payment_service.utils;

import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("paymentSecurity")
@RequiredArgsConstructor
public class PaymentSecurity {

    private final PaymentRepository paymentRepository;

    public boolean isOwner(Long paymentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return false;

        Long userId = (Long) auth.getPrincipal();
        return paymentRepository.findById(paymentId)
                .map(p -> p.getUserId().equals(userId))
                .orElse(false);
    }
}
