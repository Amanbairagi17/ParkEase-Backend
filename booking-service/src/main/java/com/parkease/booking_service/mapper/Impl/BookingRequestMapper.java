package com.parkease.booking_service.mapper.Impl;

import com.parkease.booking_service.dtos.BookingRequestDto;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingRequestMapper implements Mapper<BookingRequestDto, Booking> {

    private final ModelMapper modelMapper;

    @Override
    public BookingRequestDto mapTo(Booking booking) {
        return modelMapper.map(booking, BookingRequestDto.class);
    }

    @Override
    public Booking mapFrom(BookingRequestDto bookingRequestDto) {
        return modelMapper.map(bookingRequestDto, Booking.class);
    }
}