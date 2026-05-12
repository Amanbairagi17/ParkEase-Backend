package com.parkease.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.payment_service.dtos.PaymentRequestDto;
import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.dtos.PaymentVerificationDto;
import com.parkease.payment_service.dtos.RazorpayOrderDto;
import com.parkease.payment_service.entity.PaymentMode;
import com.parkease.payment_service.service.PaymentService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createOrder_ShouldReturnOrder() throws Exception {

        PaymentRequestDto request =
                new PaymentRequestDto();

        request.setBookingId(1L);

        request.setMode(PaymentMode.UPI);

        RazorpayOrderDto response =
                RazorpayOrderDto.builder()
                        .orderId("order_123")
                        .currency("INR")
                        .amount(1000)
                        .key("key")
                        .build();

        when(paymentService.createRazorpayOrder(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/payments/create-order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_123"))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void verifyPayment_ShouldReturnPayment() throws Exception {

        PaymentVerificationDto dto =
                new PaymentVerificationDto();

        dto.setRazorpayOrderId("order_1");

        dto.setRazorpayPaymentId("pay_1");

        dto.setRazorpaySignature("signature");

        PaymentResponseDto response =
                new PaymentResponseDto();

        response.setPaymentId(1L);

        response.setStatus("SUCCESS");

        when(paymentService.verifyPayment(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/payments/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void processPayment_ShouldReturnCreated() throws Exception {

        PaymentRequestDto request =
                new PaymentRequestDto();

        request.setBookingId(1L);

        request.setMode(PaymentMode.UPI);

        PaymentResponseDto response =
                new PaymentResponseDto();

        response.setPaymentId(1L);

        when(paymentService.processPayment(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/payments/manual")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(1));
    }

    @Test
    void getByBookingId_ShouldReturnPayment() throws Exception {

        PaymentResponseDto response =
                new PaymentResponseDto();

        response.setPaymentId(1L);

        response.setBookingId(10L);

        when(paymentService.getByBookingId(10L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/payments/booking/10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.bookingId").value(10));
    }

    @Test
    void getByUserId_ShouldReturnPayments() throws Exception {

        PaymentResponseDto response =
                new PaymentResponseDto();

        response.setPaymentId(1L);

        response.setUserId(5L);

        when(paymentService.getByUserId(5L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/payments/user/5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(1))
                .andExpect(jsonPath("$[0].userId").value(5));
    }

    @Test
    void refundPayment_ShouldReturnRefundedPayment() throws Exception {

        PaymentResponseDto response =
                new PaymentResponseDto();

        response.setPaymentId(1L);

        response.setStatus("REFUNDED");

        when(paymentService.refundPayment(1L))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/payments/1/refund")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void getPaymentStatus_ShouldReturnStatus() throws Exception {

        when(paymentService.getPaymentStatus(1L))
                .thenReturn("SUCCESS");

        mockMvc.perform(
                        get("/api/payments/1/status")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("SUCCESS"));
    }

    @Test
    void getTransactionHistory_ShouldReturnHistory() throws Exception {

        PaymentResponseDto response =
                new PaymentResponseDto();

        response.setPaymentId(1L);

        when(paymentService.getTransactionHistory(5L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/payments/history/5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(1));
    }

    @Test
    void getAllPayments_ShouldReturnPayments() throws Exception {

        PaymentResponseDto response =
                new PaymentResponseDto();

        response.setPaymentId(1L);

        when(paymentService.getAllPayments())
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/payments")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(1));
    }

    @Test
    void getTotalRevenue_ShouldReturnRevenue() throws Exception {

        when(paymentService.getTotalRevenueForUser(5L))
                .thenReturn(BigDecimal.valueOf(500));

        mockMvc.perform(
                        get("/api/payments/revenue/5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(500));
    }
}