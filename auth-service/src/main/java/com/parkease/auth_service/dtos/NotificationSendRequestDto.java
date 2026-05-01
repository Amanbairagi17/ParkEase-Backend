package com.parkease.auth_service.dtos;

import lombok.Data;

import java.util.List;

/**
 * Payload sent FROM auth-service TO notification-service POST /api/notifications/send-bulk.
 * Mirrors NotificationRequestDto in notification-service.
 */
@Data
public class NotificationSendRequestDto {
    private List<Long> recipientIds;
    private String title;
    private String message;
    private String type;       // PROMO, BOOKING, etc.
    private String channel;    // APP, EMAIL, SMS
}
