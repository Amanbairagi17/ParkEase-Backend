package com.parkease.auth_service.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Captor
    private ArgumentCaptor<HttpEntity> entityCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "apiKey", "key");
        ReflectionTestUtils.setField(emailService, "senderEmail", "sender@demo.com");
        ReflectionTestUtils.setField(emailService, "senderName", "ParkEase");
    }

    @Test
    void sendOtp_ShouldPostToBrevo() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        emailService.sendOtp("user@demo.com", "123456");

        verify(restTemplate).postForEntity(any(String.class), entityCaptor.capture(), eq(String.class));
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertEquals("key", headers.getFirst("api-key"));
    }

    @Test
    void sendVerificationEmail_ShouldPostToBrevo() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        emailService.sendVerificationEmail("user@demo.com", "https://link");

        verify(restTemplate).postForEntity(any(String.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void sendOtp_ShouldHandleException_WhenRestTemplateFails() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("fail"));

        assertDoesNotThrow(() -> emailService.sendOtp("user@demo.com", "123456"));
    }
}
