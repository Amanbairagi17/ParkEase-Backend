package com.parkease.booking_service.dtos;

import com.parkease.booking_service.entity.BookingStatus;
import com.parkease.booking_service.entity.BookingType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {

    private Long bookingId;
    private Long userId;
    private Long lotId;
    private Long spotId;
    private String vehiclePlate;
    private String vehicleType;
    private BookingType bookingType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}