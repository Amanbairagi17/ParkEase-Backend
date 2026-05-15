package com.parkease.receipt_service.utils;

import com.parkease.receipt_service.client.PaymentClient;
import com.parkease.receipt_service.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReceiptSecurity {

    private final ReceiptRepository receiptRepository;
    private final PaymentClient paymentClient;

    public boolean isOwner(String receiptId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return receiptRepository.findById(receiptId)
                .map(receipt -> receipt.getUserId().equals(currentUserId))
                .orElse(false);
    }

    public boolean isPaymentOwner(Long paymentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || paymentId == null) {
            return false;
        }

        if (receiptRepository.findByPaymentIdAndUserId(paymentId, currentUserId).isPresent()) {
            return true;
        }

        return currentUserId.equals(paymentClient.getPaymentById(paymentId).getUserId());
    }

    public boolean isCurrentUser(Long userId) {
        return SecurityUtils.isCurrentUser(userId);
    }
}
