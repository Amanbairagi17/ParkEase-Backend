package com.parkease.booking_service.event;

import com.parkease.booking_service.config.RabbitMQConfig;
import com.parkease.booking_service.dtos.PaymentSuccessEventDto;
import com.parkease.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final BookingService bookingService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEventDto event) {
        log.info("Received payment event for bookingId={}, status={}", event.getBookingId(), event.getStatus());
        if (!"SUCCESS".equalsIgnoreCase(event.getStatus())) {
            log.warn("Ignoring non-success payment event for bookingId={}, status={}",
                    event.getBookingId(), event.getStatus());
            return;
        }
        try {
            bookingService.markAsPaid(event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to update booking status for bookingId={}", event.getBookingId(), e);
        }
    }
}
