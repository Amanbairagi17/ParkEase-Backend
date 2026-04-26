package com.parkease.booking_service.utils;

import com.parkease.booking_service.repository.BookingRepository;
import org.springframework.stereotype.Component;

@Component
public class BookingSecurity {

    private final BookingRepository bookingRepository;

    public BookingSecurity(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public boolean isOwner(Long bookingId) {

        // get current logged-in user
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // get booking owner
        Long bookingUserId = bookingRepository.findUserIdByBookingId(bookingId);

        return currentUserId.equals(bookingUserId);
    }
}