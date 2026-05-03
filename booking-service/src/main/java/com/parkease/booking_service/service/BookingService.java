package com.parkease.booking_service.service;

import com.parkease.booking_service.dtos.BookingEstimateRequestDto;
import com.parkease.booking_service.dtos.BookingEstimateResponseDto;
import com.parkease.booking_service.dtos.BookingRequestDto;
import com.parkease.booking_service.dtos.BookingResponseDto;
import com.parkease.booking_service.entity.BookingType;
import com.parkease.booking_service.entity.PricingType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {

    BookingResponseDto createBooking(BookingRequestDto requestDto);

    BookingResponseDto getBookingById(Long bookingId);

    List<BookingResponseDto> getBookingsByUser(Long userId);

    List<BookingResponseDto> getBookingsByLot(Long lotId);

    List<BookingResponseDto> getActiveBookings();

    BookingResponseDto cancelBooking(Long bookingId);

    BookingResponseDto checkIn(Long bookingId);

    BookingResponseDto checkOut(Long bookingId, BigDecimal hourlyRate);

    BookingResponseDto markAsPaid(Long bookingId);

    BookingResponseDto extendBooking(Long bookingId, LocalDateTime newEndTime);

    BigDecimal calculateAmount(LocalDateTime startTime, LocalDateTime endTime, PricingType type, BigDecimal hourlyRate, BigDecimal dailyRate);

    BigDecimal calculateAmount(LocalDateTime startTime, LocalDateTime endTime, BigDecimal hourlyRate);

    List<BookingResponseDto> getBookingHistory(Long userId);

    BookingEstimateResponseDto estimateBooking(BookingEstimateRequestDto requestDto);

    BigDecimal getBookingEstimate(Long bookingId);
}
