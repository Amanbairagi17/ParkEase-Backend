package com.parkease.booking_service.mapper.Impl;

import com.parkease.booking_service.dtos.BookingResponseDto;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingResponseMapper implements Mapper<BookingResponseDto, Booking> {

    private final ModelMapper modelMapper;

    @Override
    public BookingResponseDto mapTo(Booking booking) {
        BookingResponseDto response = modelMapper.map(booking, BookingResponseDto.class);
        response.setAmount(booking.getTotalAmount());
        return response;
    }

    @Override
    public Booking mapFrom(BookingResponseDto bookingResponseDto) {
        return modelMapper.map(bookingResponseDto, Booking.class);
    }
}
