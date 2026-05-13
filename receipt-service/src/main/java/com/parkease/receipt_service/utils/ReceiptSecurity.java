package com.parkease.receipt_service.utils;

import com.parkease.receipt_service.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReceiptSecurity {

    private final ReceiptRepository receiptRepository;

    public boolean isOwner(String receiptId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return receiptRepository.findById(receiptId)
                .map(receipt -> receipt.getUserId().equals(currentUserId))
                .orElse(false);
    }
}
