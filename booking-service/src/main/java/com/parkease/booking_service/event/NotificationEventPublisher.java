package com.parkease.booking_service.event;

import com.parkease.booking_service.dtos.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE   = "notification.exchange";

    // ─routing keys  must match RabbitMQConfig in notification-service
    private static final String BOOKING_KEY  = "notification.booking";
    private static final String CHECKIN_KEY  = "notification.checkin";
    private static final String CHECKOUT_KEY = "notification.checkout";
    private static final String EXPIRY_KEY   = "notification.expiry";

    public void publishBookingConfirmed(Long userId, Long bookingId, String email) {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientId(userId)
                .type("BOOKING")
                .title("Booking Confirmed!")
                .message("Your parking spot has been reserved. Booking ID: " + bookingId)
                // if email present → send EMAIL, otherwise save as APP notification only
                .channel(email != null ? "EMAIL" : "APP")
                .relatedId(bookingId)
                .relatedType("BOOKING")
                .recipientEmail(email)
                .build();

        publish(BOOKING_KEY, event);
        log.info("Published BOOKING_CONFIRMED event. userId={}, bookingId={}", userId, bookingId);
    }

    public void publishCheckIn(Long userId, Long bookingId, String email) {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientId(userId)
                .type("CHECKIN")
                .title("Check-In Successful!")
                .message("You have checked in successfully. Booking ID: " + bookingId)
                .channel("EMAIL")
                .relatedId(bookingId)
                .relatedType("BOOKING")
                .recipientEmail(email)
                .build();

        publish(CHECKIN_KEY, event);
        log.info("Published CHECKIN event. userId={}, bookingId={}", userId, bookingId);
    }

    public void publishCheckOut(Long userId, Long bookingId, String email, String totalAmount) {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientId(userId)
                .type("CHECKOUT")
                .title("Check-Out Successful!")
                .message("You have checked out. Total amount: ₹" + totalAmount + ". Booking ID: " + bookingId)
                .channel("EMAIL")
                .relatedId(bookingId)
                .relatedType("BOOKING")
                .recipientEmail(email)
                .build();

        publish(CHECKOUT_KEY, event);
        log.info("Published CHECKOUT event. userId={}, bookingId={}", userId, bookingId);
    }

    public void publishBookingCancelled(Long userId, Long bookingId, String email) {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientId(userId)
                .type("BOOKING")
                .title("Booking Cancelled")
                .message("Your booking has been cancelled. Booking ID: " + bookingId)
                .channel("EMAIL")
                .relatedId(bookingId)
                .relatedType("BOOKING")
                .recipientEmail(email)
                .build();

        publish(BOOKING_KEY, event);
        log.info("Published BOOKING_CANCELLED event. userId={}, bookingId={}", userId, bookingId);
    }

    public void publishExpiryReminder(Long userId, Long bookingId, String email) {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientId(userId)
                .type("EXPIRY")
                .title("Parking Expiry Reminder")
                .message("Your parking booking expires in 15 minutes. Booking ID: " + bookingId)
                .channel("EMAIL")
                .relatedId(bookingId)
                .relatedType("BOOKING")
                .recipientEmail(email)
                .build();

        publish(EXPIRY_KEY, event);
        log.info("Published EXPIRY_REMINDER event. userId={}, bookingId={}", userId, bookingId);
    }

    private void publish(String routingKey, NotificationEventDto event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (Exception e) {
            // RabbitMQ down? Booking still succeeds
            log.error("Failed to publish notification. routingKey={}, error={}",
                    routingKey, e.getMessage());
        }
    }
}