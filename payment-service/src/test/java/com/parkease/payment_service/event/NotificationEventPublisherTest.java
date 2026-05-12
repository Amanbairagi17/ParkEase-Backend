package com.parkease.payment_service.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationEventPublisher publisher;

    @Test
    void publishPaymentSuccess_ShouldSendMessage() {
        publisher.publishPaymentSuccess(1L, 2L, new BigDecimal("100.00"), "txn");

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void publishPaymentSuccess_ShouldHandleRabbitError() {
        doThrow(new RuntimeException("fail"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> publisher.publishPaymentSuccess(1L, 2L, new BigDecimal("100.00"), "txn"));
    }
}
