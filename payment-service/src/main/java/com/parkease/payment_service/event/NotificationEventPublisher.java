package com.parkease.payment_service.event;

import com.parkease.payment_service.dtos.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE    = "notification.exchange";
    private static final String PAYMENT_KEY = "notification.payment";

    public void publishPaymentSuccess(Long userId, Long bookingId, BigDecimal amount, String transactionId) {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientId(userId)
                .type("PAYMENT")
                .title("Payment Successful!")
                .message(String.format("Payment of ₹%s processed successfully for Booking ID: %d. Transaction ID: %s", 
                        amount.toString(), bookingId, transactionId))
                .channel("APP") // Start with APP, can extend to EMAIL if needed
                .relatedId(bookingId)
                .relatedType("BOOKING")
                .build();

        publish(PAYMENT_KEY, event);
        log.info("Published PAYMENT_SUCCESS event. userId={}, bookingId={}", userId, bookingId);
    }

    private void publish(String routingKey, NotificationEventDto event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish payment notification. routingKey={}, error={}", 
                    routingKey, e.getMessage());
        }
    }
}
