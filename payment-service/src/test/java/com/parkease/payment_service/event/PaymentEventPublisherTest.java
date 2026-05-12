package com.parkease.payment_service.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentEventPublisher publisher;

    @Test
    void publishPaymentSuccess_ShouldSendMessage() {
        publisher.publishPaymentSuccess(10L);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void publishPaymentSuccess_ShouldHandleRabbitError() {
        doThrow(new RuntimeException("fail"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> publisher.publishPaymentSuccess(10L));
    }
}
