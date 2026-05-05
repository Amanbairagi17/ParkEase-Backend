package com.parkease.payment_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusDataFixer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void normalizeLegacyPaymentStatuses() {
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE payment
                    SET status = 'SUCCESS'
                    WHERE UPPER(status) IN ('PAID', 'COMPLETED')
                    """);
            if (updated > 0) {
                log.info("Normalized {} legacy payment status records to SUCCESS", updated);
            }
        } catch (Exception exception) {
            log.warn("Skipping legacy payment status normalization: {}", exception.getMessage());
        }
    }
}
