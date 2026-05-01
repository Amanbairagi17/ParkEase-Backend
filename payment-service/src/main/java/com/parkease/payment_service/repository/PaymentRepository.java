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

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.bookingId IN :bookingIds AND p.status = 'PAID'")
    BigDecimal sumAmountByBookingIds(@Param("bookingIds") List<Long> bookingIds);

    long countByUserId(Long userId);

    Long findUserIdByPaymentId(Long paymentId);

    Long findUserIdByBookingId(Long bookingId);
}
