package com.parkeas.notification_service.controller;

import com.parkeas.notification_service.dtos.NotificationRequestDto;
import com.parkeas.notification_service.dtos.NotificationResponseDto;
import com.parkeas.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Called by other services (booking, payment etc.)
    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody NotificationRequestDto request) {
        notificationService.send(request);
        return ResponseEntity.ok("Notification sent");
    }

    // Admin broadcast
    @PostMapping("/send-bulk")
    public ResponseEntity<String> sendBulk(
            @RequestParam List<Long> recipientIds,
            @RequestParam String title,
            @RequestParam String message) {
        notificationService.sendBulk(recipientIds, title, message);
        return ResponseEntity.ok("Bulk notification sent");
    }

    // Frontend — get all notifications for bell icon
    @GetMapping("/{recipientId}")
    public ResponseEntity<List<NotificationResponseDto>> getByRecipient(
            @PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    // Frontend — unread count for bell badge
    @GetMapping("/{recipientId}/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId));
    }

    // Mark single notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<String> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok("Marked as read");
    }

    // Mark all as read
    @PutMapping("/{recipientId}/read-all")
    public ResponseEntity<String> markAllRead(@PathVariable Long recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok("All marked as read");
    }

    // Delete a notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> delete(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok("Notification deleted");
    }
}