package com.parkease.payment_service.service.Impl;

import com.parkease.payment_service.client.BookingClient;
import com.parkease.payment_service.dtos.BookingDto;
import com.parkease.payment_service.dtos.PaymentRequestDto;
import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.dtos.PaymentVerificationDto;
import com.parkease.payment_service.dtos.RazorpayOrderDto;
import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.entity.PaymentMode;
import com.parkease.payment_service.entity.PaymentStatus;
import com.parkease.payment_service.event.NotificationEventPublisher;
import com.parkease.payment_service.event.PaymentEventPublisher;
import com.parkease.payment_service.exception.PaymentNotFoundException;
import com.parkease.payment_service.mapper.Impl.PaymentRequestMapper;
import com.parkease.payment_service.mapper.Impl.PaymentResponseMapper;
import com.parkease.payment_service.repository.PaymentRepository;
import com.parkease.payment_service.service.PaymentService;
import com.parkease.payment_service.utils.SecurityUtils;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final BookingClient bookingClient;
    private final PaymentRepository paymentRepository;
    private final PaymentResponseMapper responseMapper;
    private final PaymentRequestMapper requestMapper;
    private final NotificationEventPublisher notificationPublisher;
    private final PaymentEventPublisher paymentEventPublisher;

    @Value("${razorpay.key}")
    private String razorpayKey;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    @Value("${razorpay.webhook-secret:${razorpay.secret}}")
    private String razorpayWebhookSecret;

    private RazorpayClient client;

    @PostConstruct
    public void init() {
        try {
            if ("rzp_test_placeholder".equals(razorpayKey) || "secret_placeholder".equals(razorpaySecret)) {
                log.warn("RAZORPAY_KEY or RAZORPAY_SECRET is using default placeholder values. Payment initialization will fail.");
            }
            this.client = new RazorpayClient(razorpayKey, razorpaySecret);
            log.info("Razorpay client initialized successfully.");
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay client. Please check your credentials.", e);
        }
    }

    @Override
    @Transactional
    public RazorpayOrderDto createRazorpayOrder(PaymentRequestDto requestDto) {
        if (client == null) {
            log.error("Razorpay client not initialized. Cannot create order.");
            throw new RuntimeException("Payment service is currently unavailable: Invalid credentials");
        }
        try {
            log.info("Creating Razorpay order for bookingId={}", requestDto.getBookingId());
            
            // 1. Fetch booking details from Booking Service
            BookingDto booking = bookingClient.getBooking(requestDto.getBookingId());
            if (booking == null) {
                throw new RuntimeException("Booking not found");
            }
            
            BigDecimal amount = booking.getTotalAmount();
            log.info("Payment amount from DB: {}", amount);

            Long currentUserId = SecurityUtils.getCurrentUserId();
            String role = SecurityUtils.getCurrentUserRole();
            if (!"ADMIN".equalsIgnoreCase(role) && !booking.getUserId().equals(currentUserId)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "You are not authorized to pay for this booking");
            }

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Invalid booking amount: " + amount);
            }

            paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(requestDto.getBookingId())
                    .filter(existing -> existing.getStatus() == PaymentStatus.SUCCESS)
                    .ifPresent(existing -> {
                        if (existing.getAmount() != null && existing.getAmount().compareTo(amount) >= 0) {
                            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                                    "Payment already completed for this booking");
                        }
                    });

            // 2. Create Razorpay order
            int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + requestDto.getBookingId());

            Order order = createOrder(orderRequest);

            // 3. Save or refresh a pending payment record for this booking
            Payment payment = paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(requestDto.getBookingId())
                    .filter(existing -> existing.getStatus() == PaymentStatus.PENDING)
                    .orElseGet(() -> requestMapper.mapFrom(requestDto));
            payment.setUserId(booking.getUserId()); // Deriving from trusted source
            payment.setBookingId(booking.getBookingId());
            payment.setAmount(amount); // Use DB amount
            payment.setMode(requestDto.getMode() != null ? requestDto.getMode() : PaymentMode.UPI);
            payment.setRazorpayOrderId(order.get("id"));
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCurrency("INR");
            payment.setTransactionId(null);

            log.info("Saving pending payment for bookingId={}, userId={}, amount={}", 
                payment.getBookingId(), payment.getUserId(), payment.getAmount());
                
            paymentRepository.save(payment);

            return RazorpayOrderDto.builder()
                    .orderId(order.get("id"))
                    .amount(order.get("amount"))
                    .currency(order.get("currency"))
                    .key(razorpayKey)
                    .build();

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order", e);
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during payment initialization", e);
            throw new RuntimeException("Payment failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponseDto verifyPayment(PaymentVerificationDto verificationDto) {

        try {

            JSONObject attributes = new JSONObject();

            attributes.put("razorpay_order_id",
                    verificationDto.getRazorpayOrderId());

            attributes.put("razorpay_payment_id",
                    verificationDto.getRazorpayPaymentId());

            attributes.put("razorpay_signature",
                    verificationDto.getRazorpaySignature());

            boolean isValid =
                    Utils.verifyPaymentSignature(attributes, razorpaySecret);

            if (!isValid) {
                throw new RuntimeException("Invalid payment signature");
            }

            Payment payment = paymentRepository.findByRazorpayOrderId(
                    verificationDto.getRazorpayOrderId()
            ).orElseThrow(() ->
                    new PaymentNotFoundException(
                            "Payment not found for orderId")
            );

            Long currentUserId = SecurityUtils.getCurrentUserId();

            String role = SecurityUtils.getCurrentUserRole();

            if (!"ADMIN".equalsIgnoreCase(role)
                    && !payment.getUserId().equals(currentUserId)) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not authorized to verify this payment"
                );
            }

            // Prevent duplicate verification
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                return responseMapper.mapTo(payment);
            }

            // ===== PAYMENT SUCCESS =====

            payment.setStatus(PaymentStatus.SUCCESS);

            payment.setRazorpayPaymentId(
                    verificationDto.getRazorpayPaymentId());

            payment.setRazorpaySignature(
                    verificationDto.getRazorpaySignature());

            payment.setTransactionId(
                    verificationDto.getRazorpayPaymentId());

            payment.setPaidAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.saveAndFlush(payment);

            // ===== IMPORTANT FIX =====
            // Sync booking-service after successful payment

            try {

                bookingClient.markAsPaid(savedPayment.getBookingId());

                log.info(
                        "Booking updated successfully after payment. bookingId={}",
                        savedPayment.getBookingId()
                );

            } catch (Exception ex) {

                log.error(
                        "Failed to sync booking after payment success. bookingId={}",
                        savedPayment.getBookingId(),
                        ex
                );
            }

            notificationPublisher.publishPaymentSuccess(
                    savedPayment.getUserId(),
                    savedPayment.getBookingId(),
                    savedPayment.getAmount(),
                    savedPayment.getTransactionId()
            );

            paymentEventPublisher.publishPaymentSuccess(savedPayment);

            return responseMapper.mapTo(savedPayment);

        } catch (RazorpayException e) {

            log.error("Error verifying payment", e);

            throw new RuntimeException("Payment verification failed");
        }
    }

    @Override
    @Transactional
    public PaymentResponseDto initializePayment(PaymentRequestDto requestDto) {
        BookingDto booking = bookingClient.getBooking(requestDto.getBookingId());
        if (booking == null) {
            throw new RuntimeException("Booking not found");
        }

        if (requestDto.getIdempotencyKey() != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(requestDto.getIdempotencyKey());
            if (existing.isPresent()) {
                return responseMapper.mapTo(existing.get());
            }
        }

        BigDecimal amount = requestDto.getAmount() != null
                ? requestDto.getAmount()
                : booking.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment amount");
        }

        Payment payment = paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(requestDto.getBookingId())
                .filter(existing -> existing.getStatus() == PaymentStatus.PENDING)
                .orElseGet(Payment::new);
        payment.setBookingId(booking.getBookingId());
        payment.setUserId(booking.getUserId());
        payment.setAmount(amount);
        payment.setMode(requestDto.getMode() != null ? requestDto.getMode() : PaymentMode.UPI);
        payment.setCurrency(requestDto.getCurrency() != null ? requestDto.getCurrency() : "INR");
        payment.setDescription(requestDto.getDescription());
        payment.setIdempotencyKey(requestDto.getIdempotencyKey());
        payment.setStatus(PaymentStatus.PENDING);

        return responseMapper.mapTo(paymentRepository.save(payment));
    }

    // --- Remaining methods unchanged ---

    @Override
    public PaymentResponseDto getByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        return responseMapper.mapTo(payment);
    }

    @Override
    public PaymentResponseDto getByPaymentId(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        return responseMapper.mapTo(payment);
    }

    @Override
    public List<PaymentResponseDto> getByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only SUCCESS payments can be refunded");
        }

        try {
            if (payment.getRazorpayPaymentId() != null && !payment.getRazorpayPaymentId().isBlank()) {
                com.razorpay.Refund refund = refundPayment(payment.getRazorpayPaymentId());
                log.info("Razorpay refund processed. refundId={}", Optional.ofNullable(refund.get("id")));
            } else {
                log.info("No Razorpay payment id found. Marking offline/manual payment refunded. paymentId={}",
                        payment.getPaymentId());
            }

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            return responseMapper.mapTo(paymentRepository.save(payment));
            
        } catch (RazorpayException e) {
            log.error("Error processing Razorpay refund", e);
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponseDto refundByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return responseMapper.mapTo(payment);
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only successful payments can be refunded");
        }
        return refundPayment(payment.getPaymentId());
    }

    @Override
    @Transactional
    public PaymentResponseDto handleWebhook(String payload, String signature) {
        try {
            Utils.verifyWebhookSignature(payload, signature, razorpayWebhookSecret);
            JSONObject event = new JSONObject(payload);
            String eventName = event.optString("event");
            JSONObject payloadObject = event.optJSONObject("payload");
            JSONObject paymentObject = payloadObject == null ? null : payloadObject.optJSONObject("payment");
            JSONObject paymentEntity = paymentObject == null ? null : paymentObject.optJSONObject("entity");
            if (paymentEntity == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook payment entity missing");
            }

            String razorpayPaymentId = paymentEntity.optString("id");
            String orderId = paymentEntity.optString("order_id");
            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .or(() -> paymentRepository.findByRazorpayPaymentId(razorpayPaymentId))
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found for webhook"));

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setTransactionId(razorpayPaymentId);
            if ("payment.captured".equalsIgnoreCase(eventName) || "payment.authorized".equalsIgnoreCase(eventName)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                notificationPublisher.publishPaymentSuccess(
                        payment.getUserId(),
                        payment.getBookingId(),
                        payment.getAmount(),
                        payment.getTransactionId()
                );
                paymentEventPublisher.publishPaymentSuccess(payment);
            } else if ("payment.failed".equalsIgnoreCase(eventName)) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
            return responseMapper.mapTo(payment);
        } catch (RazorpayException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay webhook signature");
        }
    }

    protected Order createOrder(JSONObject orderRequest) throws RazorpayException {
        return client.orders.create(orderRequest);
    }

    protected com.razorpay.Refund refundPayment(String razorpayPaymentId) throws RazorpayException {
        return client.payments.refund(razorpayPaymentId);
    }

    @Override
    public String getPaymentStatus(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"))
                .getStatus().name();
    }

    @Override
    @Transactional
    public void updateStatus(Long paymentId, String status) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        payment.setStatus(PaymentStatus.fromValue(status));

        paymentRepository.save(payment);
    }

    @Override
    public BigDecimal getTotalRevenueForUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<PaymentResponseDto> getTransactionHistory(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {

        BookingDto booking = bookingClient.getBooking(requestDto.getBookingId());
        if (booking == null) {
            throw new RuntimeException("Booking not found");
        }

        BigDecimal amount = booking.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid booking amount: " + amount);
        }

        paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(requestDto.getBookingId())
            .filter(existing -> existing.getStatus() == PaymentStatus.SUCCESS)
            .ifPresent(existing -> {
                if (existing.getAmount() != null && existing.getAmount().compareTo(amount) >= 0) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Payment already completed for this booking");
                }
            });

        Payment payment = requestMapper.mapFrom(requestDto);
        payment.setBookingId(booking.getBookingId());
        payment.setUserId(booking.getUserId());
        payment.setAmount(amount);
        payment.setMode(requestDto.getMode() != null ? requestDto.getMode() : PaymentMode.UPI);

        payment.setCurrency(
                requestDto.getCurrency() != null ? requestDto.getCurrency() : "INR"
        );

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(
                UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        );
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        notificationPublisher.publishPaymentSuccess(
                saved.getUserId(),
                saved.getBookingId(),
                saved.getAmount(),
                saved.getTransactionId()
        );

        paymentEventPublisher.publishPaymentSuccess(saved);

        return responseMapper.mapTo(saved);
    }
}
