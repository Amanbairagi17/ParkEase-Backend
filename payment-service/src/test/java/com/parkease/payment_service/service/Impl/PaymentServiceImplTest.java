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
import com.razorpay.Order;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private BookingClient bookingClient;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentResponseMapper responseMapper;
    @Mock
    private PaymentRequestMapper requestMapper;
    @Mock
    private NotificationEventPublisher notificationPublisher;
    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Spy
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKey", "key");
        ReflectionTestUtils.setField(paymentService, "razorpaySecret", "secret");
    }

    @Test
    void createRazorpayOrder_ShouldThrow_WhenClientMissing() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setBookingId(10L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.createRazorpayOrder(request));

        assertTrue(ex.getMessage().contains("unavailable"));
    }

    @Test
    void createRazorpayOrder_ShouldThrow_WhenBookingMissing() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setBookingId(10L);

        ReflectionTestUtils.setField(paymentService, "client", Mockito.mock(com.razorpay.RazorpayClient.class));
        when(bookingClient.getBooking(10L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> paymentService.createRazorpayOrder(request));
    }

    @Test
    void createRazorpayOrder_ShouldCreatePendingPayment_WhenValid() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setBookingId(10L);

        ReflectionTestUtils.setField(paymentService, "client", Mockito.mock(com.razorpay.RazorpayClient.class));
        when(bookingClient.getBooking(10L)).thenReturn(new BookingDto(10L, new BigDecimal("100.00"), 5L));
        when(paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(10L)).thenReturn(Optional.empty());
        when(requestMapper.mapFrom(request)).thenReturn(new Payment());

        Order order = mock(Order.class);
        when(order.get("id")).thenReturn("order_1");
        when(order.get("amount")).thenReturn(10000);
        when(order.get("currency")).thenReturn("INR");
        doReturn(order).when(paymentService).createOrder(any(JSONObject.class));

        RazorpayOrderDto response = paymentService.createRazorpayOrder(request);

        assertEquals("order_1", response.getOrderId());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void verifyPayment_ShouldThrow_WhenPaymentMissing() throws Exception {

        PaymentVerificationDto dto =
                new PaymentVerificationDto();

        dto.setRazorpayOrderId("order");

        dto.setRazorpayPaymentId("pay");

        dto.setRazorpaySignature("sig");

        when(paymentRepository.findByRazorpayOrderId("order"))
                .thenReturn(Optional.empty());

        try (var mocked = Mockito.mockStatic(Utils.class)) {

            mocked.when(() ->
                            Utils.verifyPaymentSignature(
                                    any(JSONObject.class),
                                    anyString()
                            )
                    )
                    .thenReturn(true);

            assertThrows(
                    PaymentNotFoundException.class,
                    () -> paymentService.verifyPayment(dto)
            );
        }
    }

    @Test
    void verifyPayment_ShouldUpdatePayment_WhenValid() throws Exception {

        PaymentVerificationDto dto =
                new PaymentVerificationDto();

        dto.setRazorpayOrderId("order");

        dto.setRazorpayPaymentId("pay");

        dto.setRazorpaySignature("sig");

        Payment payment =
                new Payment();

        payment.setBookingId(10L);

        payment.setUserId(5L);

        payment.setAmount(new BigDecimal("100"));

        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByRazorpayOrderId("order"))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(responseMapper.mapTo(any(Payment.class)))
                .thenReturn(new PaymentResponseDto());

        try (var mocked = Mockito.mockStatic(Utils.class)) {

            mocked.when(() ->
                            Utils.verifyPaymentSignature(
                                    any(JSONObject.class),
                                    anyString()
                            )
                    )
                    .thenReturn(true);

            PaymentResponseDto result =
                    paymentService.verifyPayment(dto);

            assertNotNull(result);

            assertEquals(
                    PaymentStatus.SUCCESS,
                    payment.getStatus()
            );

            verify(notificationPublisher)
                    .publishPaymentSuccess(
                            anyLong(),
                            anyLong(),
                            any(BigDecimal.class),
                            anyString()
                    );

            verify(paymentEventPublisher)
                    .publishPaymentSuccess(10L);
        }
    }

    @Test
    void updateStatus_ShouldThrow_WhenMissing() {

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.updateStatus(1L, "SUCCESS")
        );
    }

    @Test
    void getByBookingId_ShouldReturnPayment() {

        Payment payment =
                new Payment();

        payment.setPaymentId(1L);

        when(paymentRepository
                .findFirstByBookingIdOrderByPaymentIdDesc(10L))
                .thenReturn(Optional.of(payment));

        when(responseMapper.mapTo(payment))
                .thenReturn(new PaymentResponseDto());

        PaymentResponseDto result =
                paymentService.getByBookingId(10L);

        assertNotNull(result);
    }

    @Test
    void processPayment_ShouldSavePayment() {

        PaymentRequestDto request =
                new PaymentRequestDto();

        request.setBookingId(10L);

        request.setMode(PaymentMode.UPI);

        BookingDto booking =
                new BookingDto(
                        10L,
                        new BigDecimal("150"),
                        5L
                );

        Payment payment =
                new Payment();

        payment.setStatus(PaymentStatus.PENDING);

        when(bookingClient.getBooking(10L))
                .thenReturn(booking);

        when(requestMapper.mapFrom(any(PaymentRequestDto.class)))
                .thenReturn(payment);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(responseMapper.mapTo(any(Payment.class)))
                .thenReturn(new PaymentResponseDto());

        PaymentResponseDto result =
                paymentService.processPayment(request);

        assertNotNull(result);

        verify(paymentRepository, times(1))
                .save(any(Payment.class));
    }

    @Test
    void verifyPayment_ShouldThrow_WhenSignatureInvalid() throws Exception {
        PaymentVerificationDto dto = new PaymentVerificationDto();
        dto.setRazorpayOrderId("order");
        dto.setRazorpayPaymentId("pay");
        dto.setRazorpaySignature("sig");

        try (var mocked = Mockito.mockStatic(Utils.class)) {
            mocked.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(false);

            assertThrows(RuntimeException.class, () -> paymentService.verifyPayment(dto));
        }
    }

    @Test
    void verifyPayment_ShouldSkipUpdate_WhenAlreadySuccess() throws Exception {
        PaymentVerificationDto dto = new PaymentVerificationDto();
        dto.setRazorpayOrderId("order");
        dto.setRazorpayPaymentId("pay");
        dto.setRazorpaySignature("sig");

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findByRazorpayOrderId("order")).thenReturn(Optional.of(payment));
        when(responseMapper.mapTo(payment)).thenReturn(new PaymentResponseDto());

        try (var mocked = Mockito.mockStatic(Utils.class)) {
            mocked.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(true);

            paymentService.verifyPayment(dto);
        }

        verify(notificationPublisher, never()).publishPaymentSuccess(anyLong(), anyLong(), any(BigDecimal.class), anyString());
        verify(paymentEventPublisher, never()).publishPaymentSuccess(anyLong());
    }

    @Test
    void refundPayment_ShouldThrow_WhenNotSuccess() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.refundPayment(1L));
    }

    @Test
    void refundPayment_ShouldUpdateStatus_WhenSuccess() throws Exception {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId("pay");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseMapper.mapTo(any(Payment.class))).thenReturn(new PaymentResponseDto());

        doReturn(mock(com.razorpay.Refund.class)).when(paymentService).refundPayment("pay");

        PaymentResponseDto response = paymentService.refundPayment(1L);

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void getByBookingId_ShouldThrow_WhenMissing() {
        when(paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(10L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getByBookingId(10L));
    }

    @Test
    void getByUserId_ShouldMapPayments() {
        when(paymentRepository.findByUserId(5L)).thenReturn(List.of(new Payment()));
        when(responseMapper.mapTo(any(Payment.class))).thenReturn(new PaymentResponseDto());

        List<PaymentResponseDto> result = paymentService.getByUserId(5L);

        assertEquals(1, result.size());
    }

    @Test
    void getPaymentStatus_ShouldReturnStatusName() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        String status = paymentService.getPaymentStatus(1L);

        assertEquals("SUCCESS", status);
    }

    @Test
    void updateStatus_ShouldPersistStatus() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.updateStatus(1L, "SUCCESS");

        verify(paymentRepository).save(payment);
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
    }

    @Test
    void getTotalRevenueForUser_ShouldSumSuccessPayments() {
        Payment success = new Payment();
        success.setStatus(PaymentStatus.SUCCESS);
        success.setAmount(new BigDecimal("50.00"));
        Payment failed = new Payment();
        failed.setStatus(PaymentStatus.FAILED);
        failed.setAmount(new BigDecimal("30.00"));

        when(paymentRepository.findByUserId(1L)).thenReturn(List.of(success, failed));

        BigDecimal total = paymentService.getTotalRevenueForUser(1L);

        assertEquals(new BigDecimal("50.00"), total);
    }

    @Test
    void getTransactionHistory_ShouldMapPayments() {
        when(paymentRepository.findByUserId(5L)).thenReturn(List.of(new Payment()));
        when(responseMapper.mapTo(any(Payment.class))).thenReturn(new PaymentResponseDto());

        List<PaymentResponseDto> result = paymentService.getTransactionHistory(5L);

        assertEquals(1, result.size());
    }

    @Test
    void getAllPayments_ShouldMapAll() {
        when(paymentRepository.findAll()).thenReturn(List.of(new Payment()));
        when(responseMapper.mapTo(any(Payment.class))).thenReturn(new PaymentResponseDto());

        List<PaymentResponseDto> result = paymentService.getAllPayments();

        assertEquals(1, result.size());
    }

    @Test
    void processPayment_ShouldThrow_WhenBookingMissing() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setBookingId(10L);
        when(bookingClient.getBooking(10L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> paymentService.processPayment(request));
    }

    @Test
    void processPayment_ShouldCreateSuccessPayment_WhenValid() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setBookingId(10L);
        request.setMode(PaymentMode.UPI);
        when(bookingClient.getBooking(10L)).thenReturn(new BookingDto(10L, new BigDecimal("120.00"), 5L));
        when(requestMapper.mapFrom(request)).thenReturn(new Payment());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseMapper.mapTo(any(Payment.class))).thenReturn(new PaymentResponseDto());

        paymentService.processPayment(request);

        verify(notificationPublisher).publishPaymentSuccess(anyLong(), anyLong(), any(BigDecimal.class), anyString());
        verify(paymentEventPublisher).publishPaymentSuccess(10L);
    }
}
