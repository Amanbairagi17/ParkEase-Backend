package com.parkeas.notification_service.listener;

import com.parkeas.notification_service.config.RabbitMQConfig;
import com.parkeas.notification_service.dtos.NotificationRequestDto;
import com.parkeas.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.BOOKING_QUEUE)
    public void handleBooking(NotificationRequestDto request) {
        process(request, "BOOKING");
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handlePayment(NotificationRequestDto request) {
        process(request, "PAYMENT");
    }

    @RabbitListener(queues = RabbitMQConfig.CHECKIN_QUEUE)
    public void handleCheckin(NotificationRequestDto request) {
        process(request, "CHECKIN");
    }

    @RabbitListener(queues = RabbitMQConfig.CHECKOUT_QUEUE)
    public void handleCheckout(NotificationRequestDto request) {
        process(request, "CHECKOUT");
    }

    @RabbitListener(queues = RabbitMQConfig.EXPIRY_QUEUE)
    public void handleExpiry(NotificationRequestDto request) {
        process(request, "EXPIRY");
    }

    private void process(NotificationRequestDto request, String type) {
        log.info("Received {} notification event for recipientId={}", type, request.getRecipientId());
        try {
            notificationService.send(request);
        } catch (Exception e) {
            log.error("Failed to process {} notification for recipientId={} : {}", type, request.getRecipientId(), e.getMessage());
        }
    }
}