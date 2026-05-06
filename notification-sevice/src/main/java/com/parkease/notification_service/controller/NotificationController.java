package com.parkease.notification_service.controller;

import com.parkease.notification_service.dtos.BulkNotificationRequestDto;
import com.parkease.notification_service.dtos.NotificationRequestDto;
import com.parkease.notification_service.dtos.NotificationResponseDto;
import com.parkease.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody NotificationRequestDto request) {
        notificationService.send(request);
        return ResponseEntity.ok("Notification sent");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/send-bulk")
    public ResponseEntity<String> sendBulk(@RequestBody BulkNotificationRequestDto request) {
        notificationService.sendBulk(request.getRecipientIds(), request.getTitle(), request.getMessage());
        return ResponseEntity.ok("Bulk notification sent");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @GetMapping("/{recipientId}")
    public ResponseEntity<List<NotificationResponseDto>> getByRecipient(
            @PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @GetMapping("/{recipientId}/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<String> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok("Marked as read");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @PutMapping("/{recipientId}/read-all")
    public ResponseEntity<String> markAllRead(@PathVariable Long recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok("All marked as read");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> delete(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok("Notification deleted");
    }
}