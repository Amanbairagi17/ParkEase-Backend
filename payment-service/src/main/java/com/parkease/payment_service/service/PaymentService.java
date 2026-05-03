package com.parkease.payment_service.service;

import com.parkease.payment_service.dtos.PaymentRequestDto;
import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.dtos.PaymentVerificationDto;
import com.parkease.payment_service.dtos.RazorpayOrderDto;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto processPayment(PaymentRequestDto requestDto);

    RazorpayOrderDto createRazorpayOrder(PaymentRequestDto requestDto);

    PaymentResponseDto verifyPayment(PaymentVerificationDto verificationDto);

    PaymentResponseDto getByBookingId(Long bookingId);

    List<PaymentResponseDto> getByUserId(Long userId);

    PaymentResponseDto refundPayment(Long paymentId);

    String getPaymentStatus(Long paymentId);

    void updateStatus(Long paymentId, String status);

    BigDecimal getTotalRevenueForUser(Long userId);

    List<PaymentResponseDto> getTransactionHistory(Long userId);

    List<PaymentResponseDto> getAllPayments();
}
