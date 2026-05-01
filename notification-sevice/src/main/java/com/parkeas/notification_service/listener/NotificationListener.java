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
        log.info("Received BOOKING notification event for recipientId={}",
                request.getRecipientId());
        notificationService.send(request);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handlePayment(NotificationRequestDto request) {
        log.info("Received PAYMENT notification event for recipientId={}",
                request.getRecipientId());
        notificationService.send(request);
    }

    @RabbitListener(queues = RabbitMQConfig.CHECKIN_QUEUE)
    public void handleCheckin(NotificationRequestDto request) {
        log.info("Received CHECKIN notification event for recipientId={}",
                request.getRecipientId());
        notificationService.send(request);
    }

    @RabbitListener(queues = RabbitMQConfig.CHECKOUT_QUEUE)
    public void handleCheckout(NotificationRequestDto request) {
        log.info("Received CHECKOUT notification event for recipientId={}",
                request.getRecipientId());
        notificationService.send(request);
    }

    @RabbitListener(queues = RabbitMQConfig.EXPIRY_QUEUE)
    public void handleExpiry(NotificationRequestDto request) {
        log.info("Received EXPIRY notification event for recipientId={}",
                request.getRecipientId());
        notificationService.send(request);
    }
}