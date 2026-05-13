package com.parkease.receipt_service.service;

import com.parkease.receipt_service.client.BookingClient;
import com.parkease.receipt_service.client.ParkingLotClient;
import com.parkease.receipt_service.client.ParkingSpotClient;
import com.parkease.receipt_service.client.PaymentClient;
import com.parkease.receipt_service.client.UserClient;
import com.parkease.receipt_service.dtos.BookingResponseDto;
import com.parkease.receipt_service.dtos.ParkingLotResponseDto;
import com.parkease.receipt_service.dtos.ParkingSpotResponseDto;
import com.parkease.receipt_service.dtos.PaymentResponseDto;
import com.parkease.receipt_service.dtos.PaymentSuccessEventDto;
import com.parkease.receipt_service.dtos.ReceiptResponseDto;
import com.parkease.receipt_service.dtos.UserResponseDto;
import com.parkease.receipt_service.entity.Receipt;
import com.parkease.receipt_service.exception.BookingNotFoundException;
import com.parkease.receipt_service.exception.DuplicateReceiptException;
import com.parkease.receipt_service.exception.InvalidPaymentStatusException;
import com.parkease.receipt_service.exception.PaymentNotFoundException;
import com.parkease.receipt_service.exception.PdfGenerationException;
import com.parkease.receipt_service.exception.ReceiptNotFoundException;
import com.parkease.receipt_service.repository.ReceiptRepository;
import com.parkease.receipt_service.utils.PdfGenerator;
import com.parkease.receipt_service.utils.ReceiptNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final UserClient userClient;
    private final PdfGenerator pdfGenerator;
    private final ModelMapper modelMapper;

    @Value("${receipt.storage.path:receipts}")
    private String storagePath;

    @Value("${receipt.download.base-url:/api/receipts/download}")
    private String downloadBaseUrl;

    @Override
    @Transactional
    public ReceiptResponseDto generateReceipt(Long paymentId) {
        if (paymentId == null) {
            throw new PaymentNotFoundException("Payment id is required");
        }

        receiptRepository.findByPaymentId(paymentId)
                .ifPresent(receipt -> {
                    throw new DuplicateReceiptException("Receipt already generated for paymentId=" + paymentId);
                });

        PaymentResponseDto payment = paymentClient.getPaymentById(paymentId);
        if (payment == null) {
            throw new PaymentNotFoundException("Payment not found: " + paymentId);
        }

        if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            throw new InvalidPaymentStatusException("Payment status is not SUCCESS: " + payment.getStatus());
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

        receiptRepository.findByPaymentId(event.getPaymentId())
                .ifPresent(receipt -> {
                    log.info("Receipt already exists for paymentId={}", event.getPaymentId());
                    throw new DuplicateReceiptException("Receipt already generated for paymentId=" + event.getPaymentId());
                });

        if (event.getBookingId() == null) {
            throw new BookingNotFoundException("Booking id is required");
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

        if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            throw new InvalidPaymentStatusException("Payment status is not SUCCESS: " + payment.getStatus());
        }

        return createReceipt(payment, booking);
    }

    @Override
    public ReceiptResponseDto getReceipt(String receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ReceiptNotFoundException("Receipt not found: " + receiptId));
        return toResponse(receipt);
    }

    @Override
    public List<ReceiptResponseDto> getReceiptsByUser(Long userId) {
        return receiptRepository.findByUserIdOrderByGeneratedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public byte[] downloadReceipt(String receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ReceiptNotFoundException("Receipt not found: " + receiptId));

        if (receipt.getPdfPath() == null) {
            throw new ReceiptNotFoundException("Receipt PDF not available");
        }

        Path path = Paths.get(receipt.getPdfPath());
        if (!Files.exists(path)) {
            throw new ReceiptNotFoundException("Receipt PDF not found on disk");
        }

        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new PdfGenerationException("Unable to read receipt PDF", ex);
        }
    }

    private ReceiptResponseDto createReceipt(PaymentResponseDto payment, BookingResponseDto booking) {
        Receipt receipt = new Receipt();
        receipt.setPaymentId(payment.getPaymentId());
        receipt.setBookingId(payment.getBookingId());
        receipt.setUserId(payment.getUserId());
        receipt.setVehicleNumber(booking.getVehiclePlate());
        receipt.setCheckInTime(booking.getStartTime());
        receipt.setCheckOutTime(booking.getEndTime());
        receipt.setDuration(booking.getDuration());
        receipt.setAmountPaid(payment.getAmount() != null ? payment.getAmount() : booking.getTotalAmount());
        receipt.setPaymentMethod(payment.getMode());
        receipt.setPaymentStatus(payment.getStatus());
        receipt.setTransactionId(payment.getTransactionId());
        receipt.setRazorpayOrderId(payment.getRazorpayOrderId());
        receipt.setRazorpayPaymentId(payment.getRazorpayPaymentId());
        receipt.setGeneratedAt(LocalDateTime.now());

        ParkingLotResponseDto lot = parkingLotClient.getLot(booking.getLotId());
        if (lot != null) {
            receipt.setParkingName(lot.getName());
        } else {
            receipt.setParkingName("Unknown Parking");
        }

        ParkingSpotResponseDto spot = parkingSpotClient.getSpot(booking.getSpotId());
        if (spot != null) {
            receipt.setSlotNumber(spot.getSpotNumber());
        } else {
            receipt.setSlotNumber("N/A");
        }

        Receipt saved;
        try {
            saved = receiptRepository.save(receipt);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateReceiptException("Receipt already generated for paymentId=" + payment.getPaymentId());
        }
        saved.setReceiptNumber(ReceiptNumberGenerator.generate(payment.getPaymentId(), saved.getReceiptId()));

        Receipt persisted = receiptRepository.save(saved);

        UserResponseDto user = null;
        if (payment.getUserId() != null) {
            user = userClient.getUserById(payment.getUserId());
        }

        byte[] pdfBytes = pdfGenerator.generateReceiptPdf(persisted, user);
        String pdfPath = storePdf(persisted, pdfBytes);
        persisted.setPdfPath(pdfPath);

        Receipt finalReceipt = receiptRepository.save(persisted);
        return toResponse(finalReceipt);
    }

    private String storePdf(Receipt receipt, byte[] pdfBytes) {
        try {
            Path basePath = Paths.get(storagePath);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            String fileName = receipt.getReceiptNumber() + ".pdf";
            Path filePath = basePath.resolve(fileName);
            Files.write(filePath, pdfBytes);
            return filePath.toAbsolutePath().toString();
        } catch (IOException ex) {
            throw new PdfGenerationException("Failed to store receipt PDF", ex);
        }
    }

    private ReceiptResponseDto toResponse(Receipt receipt) {
        ReceiptResponseDto response = modelMapper.map(receipt, ReceiptResponseDto.class);
        response.setDownloadUrl(downloadBaseUrl + "/" + receipt.getReceiptId());
        return response;
    }
}
