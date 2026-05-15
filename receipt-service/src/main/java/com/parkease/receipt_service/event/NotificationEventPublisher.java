package com.parkease.receipt_service.event;

import com.parkease.receipt_service.dtos.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "notification.exchange";
    private static final String PAYMENT_KEY = "notification.payment";

    public void publishReceiptGenerated(Long userId, Long bookingId, String receiptNumber) {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientId(userId)
                .type("RECEIPT")
                .title("Receipt Generated")
                .message("Your receipt is ready for Booking ID: " + bookingId + " (Receipt: " + receiptNumber + ")")
                .channel("APP")
                .relatedId(bookingId)
                .relatedType("BOOKING")
                .build();

        publish(PAYMENT_KEY, event);
        log.info("Published RECEIPT_GENERATED event. userId={}, bookingId={}", userId, bookingId);
    }

    private void publish(String routingKey, NotificationEventDto event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish receipt notification. routingKey={}, error={}",
                    routingKey, e.getMessage());
        }
    }
}
