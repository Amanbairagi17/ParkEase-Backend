package com.parkease.booking_service.event;

import com.parkease.booking_service.dtos.NotificationEventDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void publishBookingConfirmed_ShouldSendMessage() {
        publisher.publishBookingConfirmed(1L, 2L, "user@demo.com");

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

        @Test
        void publishBookingConfirmed_ShouldUseAppChannel_WhenEmailMissing() {
                ArgumentCaptor<NotificationEventDto> captor = ArgumentCaptor.forClass(NotificationEventDto.class);

                publisher.publishBookingConfirmed(1L, 2L, null);

                verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());

                assertEquals("APP", captor.getValue().getChannel());
        }

    @Test
    void publishCheckOut_ShouldHandleRabbitFailure() {
        doThrow(new RuntimeException("fail"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> publisher.publishCheckOut(1L, 2L, null, "100"));
    }

    @Test
    void publishBookingCancelled_ShouldSendMessage() {

        publisher.publishBookingCancelled(1L, 2L, "test");

        verify(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        any(Object.class)
                );
    }

    @Test
    void publishCheckIn_ShouldSendMessage() {

        publisher.publishCheckIn(1L, 2L, "test");

        verify(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        any(Object.class)
                );
    }
    @Test
    void publishCheckOut_ShouldSendMessage() {

        publisher.publishCheckOut(1L, 2L, "test", "100");

        verify(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        any(Object.class)
                );
    }

    @Test
    void publishExpiryReminder_ShouldSendMessage() {

        publisher.publishExpiryReminder(1L, 2L, "test");

        verify(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        any(Object.class)
                );
    }
}
