package com.parkease.notification_service.dtos;

import lombok.Data;
import java.util.List;

@Data
public class BulkNotificationRequestDto {
    private List<Long> recipientIds;
    private String title;
    private String message;
}
