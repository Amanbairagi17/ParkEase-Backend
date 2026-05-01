package com.parkease.payment_service.utils;

import com.parkease.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("paymentSecurity")
@RequiredArgsConstructor
public class PaymentSecurity {

    private final PaymentRepository paymentRepository;

    public boolean isOwner(Long paymentId) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        // ADMIN override
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        Long userId = paymentRepository.findUserIdByPaymentId(paymentId);

        return userId != null && userId.equals(currentUserId);
    }

    public boolean isBookingOwner(Long bookingId) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        Long userId = paymentRepository.findUserIdByBookingId(bookingId);

        return userId != null && userId.equals(currentUserId);
    }
}
