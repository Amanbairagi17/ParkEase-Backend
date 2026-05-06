package com.parkease.notification_service.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long recipientId;       // userId from auth-service

    @Enumerated(EnumType.STRING)
    private NotificationType type;  // BOOKING, CHECKIN, EXPIRY, CHECKOUT, PAYMENT, PROMO

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel; // APP, EMAIL, SMS

    private Long relatedId;         // bookingId or paymentId
    private String relatedType;     // "BOOKING" or "PAYMENT"

    private Boolean isRead = false;

    private LocalDateTime sentAt;
}