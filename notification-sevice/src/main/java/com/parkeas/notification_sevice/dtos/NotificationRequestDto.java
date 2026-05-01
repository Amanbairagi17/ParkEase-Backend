package com.parkeas.notification_sevice.dtos;

import com.parkeas.notification_sevice.entity.NotificationChannel;
import com.parkeas.notification_sevice.entity.NotificationType;
import lombok.Data;

@Data
public class NotificationRequestDto {

    private Long recipientId;         // who to notify

    private NotificationType type;    // BOOKING, CHECKIN, EXPIRY etc.

    private String title;             // "Booking Confirmed"

    private String message;           // "Your spot A12 is reserved for 3PM"

    private NotificationChannel channel; // APP, EMAIL, SMS

    private Long relatedId;           // bookingId or paymentId

    private String relatedType;       // "BOOKING" or "PAYMENT"

    private String recipientEmail;    // needed if channel = EMAIL

    private String recipientPhone;    // needed if channel = SMS
}