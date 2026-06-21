package com.app.modules.payment.scheduler;

import com.app.modules.payment.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs reconciliation on a schedule (hourly by default; override with
 * {@code payment.reconciliation.cron}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;

    @Scheduled(cron = "${payment.reconciliation.cron:0 0 * * * *}")
    public void run() {
        try {
            int corrected = reconciliationService.reconcile();
            log.info("Scheduled reconciliation complete — {} payment(s) corrected.", corrected);
        } catch (Exception e) {
            log.error("Scheduled reconciliation failed: {}", e.getMessage(), e);
        }
    }
}
