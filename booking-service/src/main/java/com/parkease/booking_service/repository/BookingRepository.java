package com.parkease.booking_service.repository;

import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByLotId(Long lotId);

    List<Booking> findBySpotId(Long spotId);

    List<Booking> findByStatus(BookingStatus status);

    Optional<Booking> findByBookingId(Long bookingId);

    Optional<Booking> findByVehiclePlate(String vehiclePlate);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.spotId = :spotId
              AND b.status IN (com.parkease.booking_service.entity.BookingStatus.RESERVED,
                               com.parkease.booking_service.entity.BookingStatus.ACTIVE)
            """)
    Optional<Booking> findActiveBySpotId(@Param("spotId") Long spotId);

    long countByLotIdAndStatus(Long lotId, BookingStatus status);

    @Query("SELECT b.userId FROM Booking b WHERE b.bookingId = :bookingId")

    Long findUserIdByBookingId(@Param("bookingId") Long bookingId);
}