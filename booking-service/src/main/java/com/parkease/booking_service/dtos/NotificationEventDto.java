package com.parkease.booking_service.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationEventDto {
    private Long recipientId;
    private String type;       // BOOKING, CHECKIN, CHECKOUT, EXPIRY
    private String title;
    private String message;
    private String channel;    // APP, EMAIL, SMS
    private Long relatedId;    // bookingId
    private String relatedType; // "BOOKING"
    private String recipientEmail;
}