package com.parkease.receipt_service.event;

import com.parkease.receipt_service.config.RabbitMQConfig;
import com.parkease.receipt_service.dtos.PaymentSuccessEventDto;
import com.parkease.receipt_service.exception.DuplicateReceiptException;
import com.parkease.receipt_service.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final ReceiptService receiptService;

    @RabbitListener(queues = RabbitMQConfig.RECEIPT_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEventDto event) {
        log.info("Received payment event for bookingId={}, paymentId={}", event.getBookingId(), event.getPaymentId());
        try {
            receiptService.handlePaymentSuccess(event);
        } catch (DuplicateReceiptException ex) {
            log.warn("Receipt already exists for paymentId={}", event.getPaymentId());
        } catch (Exception ex) {
            log.error("Failed to generate receipt for paymentId={}", event.getPaymentId(), ex);
        }
    }
}
