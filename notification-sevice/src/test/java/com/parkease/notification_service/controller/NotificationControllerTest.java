package com.parkease.notification_service.controller;

import com.parkease.notification_service.dtos.BulkNotificationRequestDto;
import com.parkease.notification_service.dtos.NotificationRequestDto;
import com.parkease.notification_service.dtos.NotificationResponseDto;
import com.parkease.notification_service.entity.NotificationChannel;
import com.parkease.notification_service.service.NotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @InjectMocks
    private NotificationController notificationController;

    @Mock
    private NotificationService notificationService;

    @Test
    void send_ShouldReturnOk() {

        NotificationRequestDto request =
                buildRequest();

        doNothing().when(notificationService)
                .send(any(NotificationRequestDto.class));

        ResponseEntity<String> response =
                notificationController.send(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                "Notification sent",
                response.getBody()
        );

        verify(notificationService)
                .send(any(NotificationRequestDto.class));
    }

    @Test
    void sendBulk_ShouldReturnOk() {

        BulkNotificationRequestDto request =
                new BulkNotificationRequestDto();

        request.setRecipientIds(List.of(1L, 2L));
        request.setTitle("Bulk Title");
        request.setMessage("Bulk Message");

        doNothing().when(notificationService)
                .sendBulk(
                        anyList(),
                        anyString(),
                        anyString()
                );

        ResponseEntity<String> response =
                notificationController.sendBulk(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                "Bulk notification sent",
                response.getBody()
        );

        verify(notificationService)
                .sendBulk(
                        anyList(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void getByRecipient_ShouldReturnNotifications() {

        NotificationResponseDto dto =
                new NotificationResponseDto();

        dto.setNotificationId(10L);
        dto.setTitle("Test Notification");

        when(notificationService.getByRecipient(1L))
                .thenReturn(List.of(dto));

        ResponseEntity<List<NotificationResponseDto>> response =
                notificationController.getByRecipient(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals(
                1,
                response.getBody().size()
        );

        assertEquals(
                "Test Notification",
                response.getBody().get(0).getTitle()
        );

        verify(notificationService)
                .getByRecipient(1L);
    }

    @Test
    void getUnreadCount_ShouldReturnCount() {

        when(notificationService.getUnreadCount(1L))
                .thenReturn(5);

        ResponseEntity<Integer> response =
                notificationController.getUnreadCount(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                5,
                response.getBody()
        );

        verify(notificationService)
                .getUnreadCount(1L);
    }

    @Test
    void markAsRead_ShouldReturnOk() {

        doNothing().when(notificationService)
                .markAsRead(10L);

        ResponseEntity<String> response =
                notificationController.markAsRead(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                "Marked as read",
                response.getBody()
        );

        verify(notificationService)
                .markAsRead(10L);
    }

    @Test
    void markAllRead_ShouldReturnOk() {

        doNothing().when(notificationService)
                .markAllRead(1L);

        ResponseEntity<String> response =
                notificationController.markAllRead(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                "All marked as read",
                response.getBody()
        );

        verify(notificationService)
                .markAllRead(1L);
    }

    @Test
    void delete_ShouldReturnOk() {

        doNothing().when(notificationService)
                .deleteNotification(10L);

        ResponseEntity<String> response =
                notificationController.delete(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                "Notification deleted",
                response.getBody()
        );

        verify(notificationService)
                .deleteNotification(10L);
    }

    @Test
    void getByRecipient_ShouldReturnEmptyList() {

        when(notificationService.getByRecipient(anyLong()))
                .thenReturn(List.of());

        ResponseEntity<List<NotificationResponseDto>> response =
                notificationController.getByRecipient(99L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals(
                0,
                response.getBody().size()
        );
    }

    private NotificationRequestDto buildRequest() {

        NotificationRequestDto request =
                new NotificationRequestDto();

        request.setRecipientId(1L);

        request.setTitle("Booking Confirmed");

        request.setMessage("Your booking is confirmed");

        request.setRecipientEmail("user@demo.com");

        request.setChannel(NotificationChannel.EMAIL);

        return request;
    }
}