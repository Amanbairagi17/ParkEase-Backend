package com.parkease.payment_service.service.Impl;

import com.parkease.payment_service.dtos.PaymentRequestDto;
import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.entity.PaymentStatus;
import com.parkease.payment_service.exception.PaymentNotFoundException;
import com.parkease.payment_service.mapper.Impl.PaymentRequestMapper;
import com.parkease.payment_service.mapper.Impl.PaymentResponseMapper;
import com.parkease.payment_service.repository.PaymentRepository;
import com.parkease.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentResponseMapper responseMapper;
    private final PaymentRequestMapper requestMapper;
    private final com.parkease.payment_service.event.NotificationEventPublisher notificationPublisher;

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {
        log.info("Processing payment for bookingId={}, userId={}, amount={}", 
                requestDto.getBookingId(), requestDto.getUserId(), requestDto.getAmount());

        Payment payment = requestMapper.mapFrom(requestDto);
        payment.setCurrency(requestDto.getCurrency() != null ? requestDto.getCurrency() : "INR");
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Payment processed successfully. paymentId={}, transactionId={}", 
                saved.getPaymentId(), saved.getTransactionId());

        // Publish event
        notificationPublisher.publishPaymentSuccess(
                saved.getUserId(), 
                saved.getBookingId(), 
                saved.getAmount(), 
                saved.getTransactionId()
        );

        return responseMapper.mapTo(saved);
    }

    @Override
    public PaymentResponseDto getByBookingId(Long bookingId) {
        log.info("Fetching payment for bookingId={}", bookingId);
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for bookingId: " + bookingId));
        return responseMapper.mapTo(payment);
    }

    @Override
    public List<PaymentResponseDto> getByUserId(Long userId) {
        log.info("Fetching payments for userId={}", userId);
        return paymentRepository.findByUserId(userId).stream()
                .map(responseMapper::mapTo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(Long paymentId) {
        log.info("Processing refund for paymentId={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Only PAID payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        Payment updated = paymentRepository.save(payment);
        log.info("Refund processed. paymentId={}", paymentId);
        return responseMapper.mapTo(updated);
    }

    @Override
    public String getPaymentStatus(Long paymentId) {
        log.info("Fetching payment status for paymentId={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        return payment.getStatus().name();
    }

    @Override
    @Transactional
    public void updateStatus(Long paymentId, String status) {
        log.info("Updating payment status. paymentId={}, newStatus={}", paymentId, status);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        payment.setStatus(PaymentStatus.valueOf(status));
        paymentRepository.save(payment);
    }

    @Override
    public BigDecimal getTotalRevenueForUser(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<PaymentResponseDto> getTransactionHistory(Long userId) {
        log.info("Fetching transaction history for userId={}", userId);
        return paymentRepository.findByUserId(userId).stream()
                .map(responseMapper::mapTo)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponseDto> getAllPayments() {
        log.info("Fetching all payments (admin)");
        return paymentRepository.findAll().stream()
                .map(responseMapper::mapTo)
                .collect(Collectors.toList());
    }

}
