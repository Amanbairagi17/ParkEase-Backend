package com.parkease.notification_service.service;

import com.parkease.notification_service.dtos.NotificationRequestDto;
import com.parkease.notification_service.dtos.NotificationResponseDto;
import com.parkease.notification_service.entity.Notification;
import com.parkease.notification_service.entity.NotificationChannel;
import com.parkease.notification_service.entity.NotificationType;
import com.parkease.notification_service.mapper.Impl.NotificationRequestMapper;
import com.parkease.notification_service.mapper.Impl.NotificationResponseMapper;
import com.parkease.notification_service.repository.NotificationRepository;
import com.parkease.notification_service.service.Impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationRequestMapper requestMapper;
    @Mock
    private NotificationResponseMapper responseMapper;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "apiKey", "key");
        ReflectionTestUtils.setField(service, "senderEmail", "sender@demo.com");
        ReflectionTestUtils.setField(service, "senderName", "ParkEase");
    }

    @Test
    void send_ShouldSaveNotificationAndSendEmail_WhenChannelEmail() {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setRecipientId(1L);
        request.setType(NotificationType.BOOKING);
        request.setTitle("title");
        request.setMessage("message");
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipientEmail("user@demo.com");

        when(requestMapper.mapFrom(request)).thenReturn(new Notification());
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        service.send(request);

        verify(notificationRepository).save(any(Notification.class));
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void send_ShouldHandleMappingFailure() {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setRecipientId(1L);
        request.setChannel(NotificationChannel.APP);
        when(requestMapper.mapFrom(request)).thenThrow(new RuntimeException("bad"));

        assertDoesNotThrow(() -> service.send(request));
    }

    @Test
    void sendEmail_ShouldReturn_WhenEmailInvalid() {
        assertDoesNotThrow(() -> service.sendEmail("bad", "s", "b"));
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void sendEmail_ShouldHandleHttpError() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertDoesNotThrow(() -> service.sendEmail("user@demo.com", "s", "b"));
    }

    @Test
    void sendBulk_ShouldSendAppNotifications() {
        when(requestMapper.mapFrom(any(NotificationRequestDto.class))).thenReturn(new Notification());
        service.sendBulk(List.of(1L, 2L), "t", "m");

        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void markAsRead_ShouldUpdateRepository() {
        service.markAsRead(10L);

        verify(notificationRepository).markAsRead(10L);
    }

    @Test
    void markAllRead_ShouldUpdateRepository() {
        service.markAllRead(1L);

        verify(notificationRepository).markAllRead(1L);
    }

    @Test
    void getByRecipient_ShouldReturnMappedList() {
        when(notificationRepository.findByRecipientIdOrderBySentAtDesc(1L))
                .thenReturn(List.of(new Notification()));
        when(responseMapper.mapTo(any(Notification.class))).thenReturn(new NotificationResponseDto());

        List<NotificationResponseDto> result = service.getByRecipient(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getUnreadCount_ShouldReturnCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(4);

        int count = service.getUnreadCount(1L);

        assertEquals(4, count);
    }

    @Test
    void deleteNotification_ShouldDeleteFromRepository() {
        service.deleteNotification(10L);

        verify(notificationRepository).deleteByNotificationId(10L);
    }
}
