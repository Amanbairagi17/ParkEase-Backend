package com.parkease.receipt_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceiptDownloadResponseDto {
    private String receiptId;
    private String fileName;
    private String contentType;
    private long contentLength;
}
