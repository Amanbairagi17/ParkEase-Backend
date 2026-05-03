package com.parkease.payment_service.event;

import com.parkease.payment_service.dtos.PaymentSuccessEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "payment.exchange";
    private static final String SUCCESS_KEY = "payment.success";

    public void publishPaymentSuccess(Long bookingId) {
        PaymentSuccessEventDto event = PaymentSuccessEventDto.builder()
                .bookingId(bookingId)
                .status("PAID")
                .build();

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, SUCCESS_KEY, event);
            log.info("Published payment success event for bookingId={}", bookingId);
        } catch (Exception e) {
            log.error("Failed to publish payment success event", e);
        }
    }
}
