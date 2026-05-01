package com.parkeas.notification_service.service.Impl;

import com.parkeas.notification_service.dtos.NotificationRequestDto;
import com.parkeas.notification_service.dtos.NotificationResponseDto;
import com.parkeas.notification_service.entity.Notification;
import com.parkeas.notification_service.entity.NotificationChannel;
import com.parkeas.notification_service.mapper.Impl.NotificationRequestMapper;
import com.parkeas.notification_service.mapper.Impl.NotificationResponseMapper;
import com.parkeas.notification_service.repository.NotificationRepository;
import com.parkeas.notification_service.service.NotificationService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRequestMapper requestMapper;   // RequestDto ↔ Entity
    private final NotificationResponseMapper responseMapper; // ResponseDto ↔ Entity
    private final RestTemplate restTemplate;          // ← REPLACE JavaMailSender

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;
    private HttpHeaders headers;

    /**
     * Trim all @Value-injected Brevo config strings.
     * Environment variables (from .env files, Docker secrets, or OS env)
     * frequently carry trailing newlines/spaces that @Value does NOT strip.
     * A dirty api-key header value causes intermittent 401 Unauthorized from Brevo.
     */
    @PostConstruct
    private void sanitizeBrevoConfig() {
        if (apiKey != null)      apiKey      = apiKey.trim();
        if (senderEmail != null) senderEmail = senderEmail.trim();
        if (senderName  != null) senderName  = senderName.trim();
        log.info("[Brevo] Config sanitized. senderEmail='{}', senderName='{}', apiKey starts with '{}'",
                senderEmail, senderName,
                (apiKey != null && apiKey.length() > 10) ? apiKey.substring(0, 10) + "..." : apiKey);
    }

    @Override
    public void send(NotificationRequestDto request) {

        // 1. Save to DB
        Notification notification = requestMapper.mapFrom(request);
        notification.setIsRead(false);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("Notification saved. recipientId={}, type={}",
                request.getRecipientId(), request.getType());

        // 2. Send Email if channel is EMAIL
        if (request.getChannel() == NotificationChannel.EMAIL
                && request.getRecipientEmail() != null) {
            sendEmail(
                    request.getRecipientEmail(),
                    request.getTitle(),
                    request.getMessage()
            );
        }

        // 3. SMS — add Twilio later
        if (request.getChannel() == NotificationChannel.SMS) {
            log.info("SMS not yet implemented for recipientId={}", request.getRecipientId());
        }
    }

    @Override
    public void sendBulk(List<Long> recipientIds, String title, String message) {
        recipientIds.forEach(recipientId -> {
            NotificationRequestDto request = new NotificationRequestDto();
            request.setRecipientId(recipientId);
            request.setTitle(title);
            request.setMessage(message);
            request.setChannel(NotificationChannel.APP);
            send(request);
        });
        log.info("Bulk notification sent to {} recipients", recipientIds.size());
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(Long recipientId) {
        List<Notification> unread = notificationRepository
                .findByRecipientIdAndIsRead(recipientId, false);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        log.info("Marked all as read for recipientId={}", recipientId);
    }

    @Override
    public List<NotificationResponseDto> getByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientId(recipientId)
                .stream()
                .map(responseMapper::mapTo)  // ResponseMapper: Entity → Dto
                .toList();
    }

    @Override
    public int getUnreadCount(Long recipientId) {
        return notificationRepository
                .countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteByNotificationId(notificationId);
        log.info("Notification deleted. id={}", notificationId);
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        String url = "https://api.brevo.com/v3/smtp/email";

        // ── 1. Sanitize inputs ────────────────────────────────────────────────
        String recipientEmail = (to      != null) ? to.trim()      : null;
        String emailSubject   = (subject != null) ? subject.trim() : "(no subject)";
        String emailBody      = (body    != null) ? body.trim()    : "";

        // ── 2. Pre-call validation ────────────────────────────────────────────
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            log.error("[Brevo] Skipping email — recipient address is null or empty. subject='{}'", emailSubject);
            return;
        }
        if (!recipientEmail.contains("@")) {
            log.error("[Brevo] Skipping email — invalid recipient address '{}'. subject='{}'", recipientEmail, emailSubject);
            return;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("[Brevo] Skipping email — api-key is null or empty.");
            return;
        }
        if (senderEmail == null || senderEmail.isEmpty()) {
            log.error("[Brevo] Skipping email — sender email is null or empty.");
            return;
        }

        //3. Build request payload
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sender", Map.of(
                "email", senderEmail,
                "name",  (senderName != null ? senderName : "ParkEase")
        ));
        requestBody.put("to", List.of(Map.of("email", recipientEmail)));
        requestBody.put("subject", emailSubject);
        requestBody.put("htmlContent", "<p>" + emailBody + "</p>");

        // 4. Build headers 
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);   // apiKey already trimmed in @PostConstruct

        // 5. Debug log before the call 
        log.info("[Brevo] Sending email → to='{}', subject='{}', sender='{}', apiKey='{}''",
                recipientEmail, emailSubject, senderEmail,
                apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "<short-key>");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 6. Call Brevo API 
        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);
            log.info("[Brevo] Email sent successfully → to='{}', status={}", recipientEmail, response.getStatusCode());
        } catch (HttpClientErrorException e) {
            // Log full Brevo error body (contains exact error code & message)
            log.error("[Brevo] HTTP {} sending email to '{}'. Brevo response: {}",
                    e.getStatusCode(), recipientEmail, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[Brevo] Unexpected error sending email to '{}': {}", recipientEmail, e.getMessage(), e);
        }
    }
}