package com.parkease.receipt_service.service;

import com.parkease.receipt_service.dtos.PaymentSuccessEventDto;
import com.parkease.receipt_service.dtos.ReceiptResponseDto;

import java.util.List;

public interface ReceiptService {

    ReceiptResponseDto generateReceipt(Long paymentId);

    ReceiptResponseDto handlePaymentSuccess(PaymentSuccessEventDto event);

    ReceiptResponseDto getReceipt(String receiptId);

    List<ReceiptResponseDto> getReceiptsByUser(Long userId);

    byte[] downloadReceipt(String receiptId);
}
