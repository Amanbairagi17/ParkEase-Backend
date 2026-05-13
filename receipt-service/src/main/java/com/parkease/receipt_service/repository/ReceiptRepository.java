package com.parkease.receipt_service.repository;

import com.parkease.receipt_service.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, String> {
    Optional<Receipt> findByPaymentId(Long paymentId);
    List<Receipt> findByUserIdOrderByGeneratedAtDesc(Long userId);
}
