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
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRequestMapper requestMapper;
    private final NotificationResponseMapper responseMapper;
    private final RestTemplate restTemplate;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    @PostConstruct
    private void sanitizeBrevoConfig() {
        if (apiKey != null) apiKey = apiKey.trim();
        if (senderEmail != null) senderEmail = senderEmail.trim();
        if (senderName != null) senderName = senderName.trim();
        log.info("Brevo config sanitized. senderEmail={}", senderEmail);
    }

    @Override
    public void send(NotificationRequestDto request) {
        log.info("Processing notification for recipientId={}, type={}",
                request.getRecipientId(), request.getType());

        try {
            Notification notification = requestMapper.mapFrom(request);
            notification.setIsRead(false);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Notification saved successfully for recipientId={}",
                    request.getRecipientId());

        } catch (Exception e) {
            log.error("Error saving notification for recipientId={} : {}",
                    request.getRecipientId(), e.getMessage(), e);
        }

        if (request.getChannel() == NotificationChannel.EMAIL
                && request.getRecipientEmail() != null) {

            try {
                sendEmail(
                        request.getRecipientEmail(),
                        request.getTitle(),
                        request.getMessage()
                );
            } catch (Exception e) {
                log.error("Email sending failed for recipientId={} : {}",
                        request.getRecipientId(), e.getMessage(), e);
            }
        }

        if (request.getChannel() == NotificationChannel.SMS) {
            log.info("SMS not implemented for recipientId={}",
                    request.getRecipientId());
        }
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email to {}", to);

        String url = "https://api.brevo.com/v3/smtp/email";

        try {
            if (to == null || !to.contains("@")) {
                log.error("Invalid email address: {}", to);
                return;
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sender", Map.of(
                    "email", senderEmail,
                    "name", senderName != null ? senderName : "ParkEase"
            ));
            requestBody.put("to", List.of(Map.of("email", to)));
            requestBody.put("subject", subject);
            requestBody.put("htmlContent", "<p>" + body + "</p>");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            log.info("Email sent successfully to {} with status={}",
                    to, response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("Brevo API error for {} : {}", to, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected email error for {} : {}", to, e.getMessage(), e);
        }
    }

    @Override
    public void sendBulk(List<Long> recipientIds, String title, String message) {
        log.info("Sending bulk notifications to {} users", recipientIds.size());

        recipientIds.forEach(id -> {
            NotificationRequestDto req = new NotificationRequestDto();
            req.setRecipientId(id);
            req.setTitle(title);
            req.setMessage(message);
            req.setChannel(NotificationChannel.APP);
            send(req);
        });

        log.info("Bulk notifications sent successfully");
    }

@Override
    @Transactional
    public void markAsRead(Long notificationId) {
        log.info("Marking notification as read. id={}", notificationId);

        notificationRepository.markAsRead(notificationId);

        log.info("Notification marked as read. id={}", notificationId);
    }

@Override
    @Transactional
    public void markAllRead(Long recipientId) {
        log.info("Marking all notifications as read for recipientId={}", recipientId);

        notificationRepository.markAllRead(recipientId);

        log.info("All notifications marked as read for recipientId={}", recipientId);
    }


    @Override
    public List<NotificationResponseDto> getByRecipient(Long recipientId) {
        log.info("Fetching notifications for recipientId={}", recipientId);

        List<NotificationResponseDto> result =
                notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId)
                        .stream()
                        .map(responseMapper::mapTo)
                        .toList();


        log.info("Fetched {} notifications for recipientId={}", result.size(), recipientId);
        return result;
    }

    @Override
    public int getUnreadCount(Long recipientId) {
        log.info("Fetching unread count for recipientId={}", recipientId);

        int count = notificationRepository
                .countByRecipientIdAndIsRead(recipientId, false);

        log.info("Unread count for recipientId={} is {}", recipientId, count);
        return count;
    }

@Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        log.info("Deleting notification id={}", notificationId);

        notificationRepository.deleteByNotificationId(notificationId);

        log.info("Notification deleted id={}", notificationId);
    }

}