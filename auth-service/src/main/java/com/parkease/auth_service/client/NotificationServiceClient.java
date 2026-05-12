package com.parkease.auth_service.client;

import com.parkease.auth_service.config.NotificationRabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Task 4 — Notification client.
 * auth-service publishes events to RabbitMQ for async notification delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

     private final RabbitTemplate rabbitTemplate;

    /**
     * Send a broadcast notification to a list of recipient IDs.
     *
     * @param recipientIds list of user IDs to notify
     * @param title        notification title
     * @param message      notification body
     */
    public void sendBulk(List<Long> recipientIds, String title, String message) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            log.warn("[NotificationClient] sendBulk called with empty recipientIds — skipping.");
            return;
        }

        Map<String, Object> payload = Map.of(
                "recipientIds", recipientIds,
                "title", title,
                "message", message
        );

        try {
            rabbitTemplate.convertAndSend(
                    NotificationRabbitConfig.EXCHANGE,
                    NotificationRabbitConfig.ADMIN_BROADCAST_KEY,
                    payload
            );
            log.info("[NotificationClient] Bulk notification enqueued. recipients={}", recipientIds.size());
        } catch (Exception e) {
            log.error("[NotificationClient] Failed to enqueue bulk notification: {}", e.getMessage(), e);
        }
    }


     // Send a single warning notification to a specific user.
    public void sendWarning(Long userId, String userEmail, String title, String message) {
        Map<String, Object> payload = Map.of(
                "recipientId", userId,
                "recipientEmail", userEmail,
                "title", title,
                "message", message,
                "type", "PROMO",
                "channel", "APP"
        );

        try {
            rabbitTemplate.convertAndSend(
                    NotificationRabbitConfig.EXCHANGE,
                    NotificationRabbitConfig.ADMIN_WARN_KEY,
                    payload
            );
            log.info("[NotificationClient] Warning notification enqueued for userId={}", userId);
        } catch (Exception e) {
            log.error("[NotificationClient] Failed to enqueue warning for userId={}: {}", userId, e.getMessage(), e);
        }
    }
}
