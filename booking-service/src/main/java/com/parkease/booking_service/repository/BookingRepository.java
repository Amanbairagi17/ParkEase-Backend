package com.parkease.booking_service.repository;

import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByLotId(Long lotId);

    List<Booking> findBySpotId(Long spotId);

    List<Booking> findByLotIdAndStatusIn(Long lotId, List<BookingStatus> status);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByStatusIn(List<BookingStatus> status);

    Optional<Booking> findByBookingId(Long bookingId);

    Optional<Booking> findByVehiclePlate(String vehiclePlate);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.spotId = :spotId
            AND b.status IN (
                 com.parkease.booking_service.entity.BookingStatus.RESERVED,
                 com.parkease.booking_service.entity.BookingStatus.ACTIVE)
            """)
    Optional<Booking> findActiveBySpotId(@Param("spotId") Long spotId);

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.spotId = :spotId
              AND b.status IN (
                 com.parkease.booking_service.entity.BookingStatus.RESERVED,
                 com.parkease.booking_service.entity.BookingStatus.ACTIVE)
              AND b.startTime < :endTime
              AND COALESCE(b.endTime, :endTime) > :startTime
            """)
    boolean existsOverlappingBooking(@Param("spotId") Long spotId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingId = :bookingId")
    Optional<Booking> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = com.parkease.booking_service.entity.BookingStatus.RESERVED
              AND b.startTime < :expiredBefore
            """)
    List<Booking> findExpiredReservedBookings(@Param("expiredBefore") LocalDateTime expiredBefore);


    long countByLotIdAndStatus(Long lotId, BookingStatus status);

    @Query("SELECT b.userId FROM Booking b WHERE b.bookingId = :bookingId")

    Long findUserIdByBookingId(@Param("bookingId") Long bookingId);
}
