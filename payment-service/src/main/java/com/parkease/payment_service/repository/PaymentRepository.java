package com.parkease.payment_service.repository;

import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findFirstByBookingIdOrderByPaymentIdDesc(Long bookingId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.bookingId IN :bookingIds AND p.status = com.parkease.payment_service.entity.PaymentStatus.SUCCESS")
    BigDecimal sumAmountByBookingIds(@Param("bookingIds") List<Long> bookingIds);

    long countByUserId(Long userId);

    @Query("SELECT p.userId FROM Payment p WHERE p.paymentId = :paymentId")
    Long findUserIdByPaymentId(@Param("paymentId") Long paymentId);

    @Query("""
            SELECT p.userId FROM Payment p
            WHERE p.bookingId = :bookingId
              AND p.paymentId = (
                  SELECT MAX(p2.paymentId) FROM Payment p2
                  WHERE p2.bookingId = :bookingId
              )
            """)
    Long findUserIdByBookingId(@Param("bookingId") Long bookingId);
}
