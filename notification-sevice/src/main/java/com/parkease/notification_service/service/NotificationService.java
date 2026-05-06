package com.parkease.notification_service.service;

import com.parkease.notification_service.dtos.NotificationRequestDto;
import com.parkease.notification_service.dtos.NotificationResponseDto;

import java.util.List;

public interface NotificationService {

    void send(NotificationRequestDto request);

    void sendBulk(List<Long> recipientIds, String title, String message);

    void markAsRead(Long notificationId);

    void markAllRead(Long recipientId);

    List<NotificationResponseDto> getByRecipient(Long recipientId);

    int getUnreadCount(Long recipientId);

    void deleteNotification(Long notificationId);

    void sendEmail(String to, String subject, String body);
}