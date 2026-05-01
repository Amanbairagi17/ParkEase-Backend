package com.parkease.auth_service.client;

import com.parkease.auth_service.dtos.NotificationSendRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Task 4 — Notification client.
 * auth-service calls notification-service directly via RestTemplate
 * (no Feign needed since auth-service doesn't have Feign on the classpath).
 *
 * The call uses the /api/notifications/send-bulk endpoint which requires ROLE_ADMIN.
 * We inject X-User-Roles: ROLE_ADMIN as an internal service header so the
 * notification-service HeaderAuthenticationFilter accepts the request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url:http://localhost:8086}")
    private String notificationServiceUrl;

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

        // Build payload for POST /api/notifications/send-bulk
        // The notification-service sendBulk accepts recipientIds, title, message as @RequestParam
        String url = notificationServiceUrl + "/api/notifications/send-bulk"
                + "?title=" + encode(title)
                + "&message=" + encode(message)
                + "&recipientIds=" + String.join(",", recipientIds.stream().map(String::valueOf).toList());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Inject internal service auth headers so notification-service allows the request
        headers.set("X-User-Id", "0");
        headers.set("X-User-Roles", "ROLE_ADMIN");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);
            log.info("[NotificationClient] Bulk notification dispatched. status={}, recipients={}",
                    response.getStatusCode(), recipientIds.size());
        } catch (Exception e) {
            log.error("[NotificationClient] Failed to dispatch bulk notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send a single warning notification to a specific user.
     */
    public void sendWarning(Long userId, String userEmail, String title, String message) {
        // Use POST /api/notifications/send (open endpoint, no auth required)
        String url = notificationServiceUrl + "/api/notifications/send";

        NotificationSendRequestDto payload = new NotificationSendRequestDto();
        payload.setRecipientIds(List.of(userId));
        payload.setTitle(title);
        payload.setMessage(message);
        payload.setType("PROMO");
        payload.setChannel("APP");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build a single-recipient body that matches NotificationRequestDto
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("recipientId", userId);
        body.put("recipientEmail", userEmail);
        body.put("title", title);
        body.put("message", message);
        body.put("type", "PROMO");
        body.put("channel", "APP");

        HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);
            log.info("[NotificationClient] Warning notification sent to userId={}. status={}",
                    userId, response.getStatusCode());
        } catch (Exception e) {
            log.error("[NotificationClient] Failed to send warning to userId={}: {}", userId, e.getMessage(), e);
        }
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
