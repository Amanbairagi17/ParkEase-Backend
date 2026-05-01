package com.parkease.payment_service.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEventDto {
    private Long recipientId;
    private String type;       // BOOKING, PAYMENT, etc.
    private String title;
    private String message;
    private String channel;    // APP, EMAIL, SMS
    private Long relatedId;    
    private String relatedType; 
    private String recipientEmail;
}
