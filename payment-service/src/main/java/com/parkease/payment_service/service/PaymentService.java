package com.parkease.payment_service.service;

import com.parkease.payment_service.dtos.PaymentRequestDto;
import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.dtos.PaymentVerificationDto;
import com.parkease.payment_service.dtos.RazorpayOrderDto;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto processPayment(PaymentRequestDto requestDto);

    PaymentResponseDto initializePayment(PaymentRequestDto requestDto);

    RazorpayOrderDto createRazorpayOrder(PaymentRequestDto requestDto);

    PaymentResponseDto verifyPayment(PaymentVerificationDto verificationDto);

    PaymentResponseDto getByBookingId(Long bookingId);

    PaymentResponseDto getByPaymentId(Long paymentId);

    List<PaymentResponseDto> getByUserId(Long userId);

    PaymentResponseDto refundPayment(Long paymentId);

    PaymentResponseDto refundByBookingId(Long bookingId);

    PaymentResponseDto handleWebhook(String payload, String signature);

    String getPaymentStatus(Long paymentId);

    void updateStatus(Long paymentId, String status);

    BigDecimal getTotalRevenueForUser(Long userId);

    List<PaymentResponseDto> getTransactionHistory(Long userId);

    List<PaymentResponseDto> getAllPayments();
}
