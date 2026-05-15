package com.parkease.receipt_service.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationEventDto {
    private Long recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;
    private Long relatedId;
    private String relatedType;
    private String recipientEmail;
}
