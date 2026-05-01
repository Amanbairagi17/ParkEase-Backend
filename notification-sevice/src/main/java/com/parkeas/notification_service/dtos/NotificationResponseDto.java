package com.parkeas.notification_service.dtos;

import com.parkeas.notification_service.entity.NotificationChannel;
import com.parkeas.notification_service.entity.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDto {

    private Long notificationId;

    private String title;

    private String message;

    private NotificationType type;

    private NotificationChannel channel;

    private Boolean isRead;

    private LocalDateTime sentAt;

    private Long relatedId;

    private String relatedType;
}