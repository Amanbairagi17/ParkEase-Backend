package com.parkease.receipt_service.service.Impl;

import com.parkease.receipt_service.client.BookingClient;
import com.parkease.receipt_service.client.ParkingLotClient;
import com.parkease.receipt_service.client.ParkingSpotClient;
import com.parkease.receipt_service.client.PaymentClient;
import com.parkease.receipt_service.dtos.BookingResponseDto;
import com.parkease.receipt_service.dtos.ParkingLotResponseDto;
import com.parkease.receipt_service.dtos.ParkingSpotResponseDto;
import com.parkease.receipt_service.dtos.PaymentResponseDto;
import com.parkease.receipt_service.dtos.PaymentSuccessEventDto;
import com.parkease.receipt_service.dtos.ReceiptResponseDto;
import com.parkease.receipt_service.entity.Receipt;
import com.parkease.receipt_service.event.NotificationEventPublisher;
import com.parkease.receipt_service.exception.BookingNotFoundException;
import com.parkease.receipt_service.exception.DuplicateReceiptException;
import com.parkease.receipt_service.exception.PaymentNotFoundException;
import com.parkease.receipt_service.exception.ReceiptNotFoundException;
import com.parkease.receipt_service.repository.ReceiptRepository;
import com.parkease.receipt_service.service.ReceiptService;
import com.parkease.receipt_service.utils.ReceiptNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PaymentClient paymentClient;
    private final BookingClient bookingClient;
    private final ParkingLotClient parkingLotClient;
    private final ParkingSpotClient parkingSpotClient;
    private final ModelMapper modelMapper;
    private final NotificationEventPublisher notificationPublisher;

    @Override
    @Transactional
    public ReceiptResponseDto generateReceipt(Long paymentId) {
        receiptRepository.findByPaymentId(paymentId)
                .ifPresent(receipt -> {
                    throw new DuplicateReceiptException("Receipt already exists for payment: " + paymentId);
                });

        PaymentResponseDto payment = paymentClient.getPaymentById(paymentId);
        if (payment == null) {
            throw new PaymentNotFoundException("Payment not found: " + paymentId);
        }

        BookingResponseDto booking = bookingClient.getBooking(payment.getBookingId());
        if (booking == null) {
            throw new BookingNotFoundException("Booking not found: " + payment.getBookingId());
        }

        return createReceipt(payment, booking);
    }

    @Override
    @Transactional
    public ReceiptResponseDto handlePaymentSuccess(PaymentSuccessEventDto event) {
        if (event == null || event.getPaymentId() == null) {
            throw new PaymentNotFoundException("Payment id is required");
        }

        if (receiptRepository.findByPaymentId(event.getPaymentId()).isPresent()) {
            log.info("Receipt already exists for paymentId={}", event.getPaymentId());
            return toResponse(receiptRepository.findByPaymentId(event.getPaymentId()).get());
        }

        BookingResponseDto booking = bookingClient.getBooking(event.getBookingId());
        if (booking == null) {
            throw new BookingNotFoundException("Booking not found: " + event.getBookingId());
        }

        PaymentResponseDto payment = new PaymentResponseDto();
        payment.setPaymentId(event.getPaymentId());
        payment.setBookingId(event.getBookingId());
        payment.setUserId(event.getUserId());
        payment.setAmount(event.getAmount());
        payment.setStatus(event.getStatus());
        payment.setMode(event.getPaymentMode());
        payment.setTransactionId(event.getTransactionId());
        payment.setRazorpayOrderId(event.getRazorpayOrderId());
        payment.setRazorpayPaymentId(event.getRazorpayPaymentId());
        payment.setPaidAt(LocalDateTime.now());

        return createReceipt(payment, booking);
    }

    @Override
    public ReceiptResponseDto getReceipt(String receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ReceiptNotFoundException("Receipt not found: " + receiptId));
        return toResponse(receipt);
    }

    @Override
    public ReceiptResponseDto getReceiptByPayment(Long paymentId) {
        Receipt receipt = receiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ReceiptNotFoundException("Receipt not found for payment: " + paymentId));
        return toResponse(receipt);
    }

    @Override
    public List<ReceiptResponseDto> getReceiptsByUser(Long userId) {
        return receiptRepository.findByUserIdOrderByGeneratedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ReceiptResponseDto createReceipt(PaymentResponseDto payment, BookingResponseDto booking) {
        Receipt receipt = new Receipt();
        receipt.setPaymentId(payment.getPaymentId());
        receipt.setBookingId(payment.getBookingId());
        receipt.setUserId(payment.getUserId());
        receipt.setVehicleNumber(booking.getVehiclePlate());
        
        LocalDateTime checkInTime = booking.getCheckInTime() != null ? booking.getCheckInTime() : booking.getStartTime();
        LocalDateTime checkOutTime = booking.getCheckOutTime() != null ? booking.getCheckOutTime() : booking.getEndTime();
        
        receipt.setCheckInTime(checkInTime);
        receipt.setCheckOutTime(checkOutTime);
        receipt.setDuration(booking.getDuration());
        receipt.setBookingType(booking.getBookingType());
        receipt.setPricingType(booking.getPricingType());
        
        BigDecimal totalAmount = payment.getAmount() != null ? payment.getAmount() : booking.getTotalAmount();
        receipt.setAmountPaid(totalAmount);
        applyTaxBreakup(receipt, totalAmount);
        
        receipt.setPaymentMethod(payment.getMode());
        receipt.setPaymentStatus(payment.getStatus());
        receipt.setTransactionId(payment.getTransactionId());
        receipt.setRazorpayOrderId(payment.getRazorpayOrderId());
        receipt.setRazorpayPaymentId(payment.getRazorpayPaymentId());
        receipt.setPaymentTime(payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now());
        receipt.setGeneratedAt(LocalDateTime.now());

        ParkingLotResponseDto lot = parkingLotClient.getLot(booking.getLotId());
        receipt.setParkingName(lot != null ? lot.getName() : "Unknown Parking");

        ParkingSpotResponseDto spot = parkingSpotClient.getSpot(booking.getSpotId());
        receipt.setSlotNumber(spot != null ? spot.getSpotNumber() : "N/A");

        try {

            receipt.setReceiptNumber(
                    "RCT-" + System.currentTimeMillis()
            );

            Receipt persisted =
                    receiptRepository.save(receipt);

            notificationPublisher.publishReceiptGenerated(
                    persisted.getUserId(),
                    persisted.getBookingId(),
                    persisted.getReceiptNumber()
            );

            return toResponse(persisted);

        } catch (DataIntegrityViolationException ex) {

            throw new DuplicateReceiptException("Receipt already generated for paymentId=" + payment.getPaymentId());
        }
    }

    private void applyTaxBreakup(Receipt receipt, BigDecimal totalAmount) {
        if (totalAmount == null) {
            receipt.setBaseAmount(BigDecimal.ZERO);
            receipt.setServiceCharge(BigDecimal.ZERO);
            receipt.setGstAmount(BigDecimal.ZERO);
            return;
        }

        BigDecimal serviceRate = BigDecimal.valueOf(0.02);
        BigDecimal gstRate = BigDecimal.valueOf(0.18);
        BigDecimal divisor = BigDecimal.ONE.add(serviceRate).add(gstRate);
        BigDecimal baseAmount = totalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal serviceCharge = baseAmount.multiply(serviceRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = totalAmount.subtract(baseAmount).subtract(serviceCharge).setScale(2, RoundingMode.HALF_UP);

        receipt.setBaseAmount(baseAmount);
        receipt.setServiceCharge(serviceCharge);
        receipt.setGstAmount(gstAmount);
    }

    private ReceiptResponseDto toResponse(Receipt receipt) {
        return modelMapper.map(receipt, ReceiptResponseDto.class);
    }
}

