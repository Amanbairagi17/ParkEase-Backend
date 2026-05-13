package com.parkease.receipt_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"paymentId"}),
        @UniqueConstraint(columnNames = {"receiptNumber"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
public class Receipt {

    @Id
    @Column(length = 36)
    private String receiptId;

    @Column(nullable = false, unique = true)
    private String receiptNumber;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String vehicleNumber;

    @Column(nullable = false)
    private String parkingName;

    @Column(nullable = false)
    private String slotNumber;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private String duration;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    private String paymentMethod;

    private String paymentStatus;

    private String transactionId;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private LocalDateTime generatedAt;

    private String pdfPath;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (receiptId == null) {
            receiptId = UUID.randomUUID().toString();
        }
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
