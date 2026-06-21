package com.app.modules.payment.service.impl;

import com.app.infrastructure.lock.AdvisoryLockService;
import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.entity.ReconciliationLog;
import com.app.modules.payment.enums.PaymentStatus;
import com.app.modules.payment.metrics.PaymentMetrics;
import com.app.modules.payment.repository.PaymentRepository;
import com.app.modules.payment.repository.ReconciliationLogRepository;
import com.app.modules.payment.service.PspStatusVerifier;
import com.app.modules.payment.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Finds payments stuck in PROCESSING beyond a threshold, queries the PSP for their authoritative
 * status, and corrects the local state — writing a {@link ReconciliationLog} for each change.
 *
 * <p>Guarded by a global transaction-scoped advisory lock so overlapping schedules (or multiple
 * instances) never reconcile concurrently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    /** Distinct from any payment id (which are positive) so the keyspaces never collide. */
    private static final long RECONCILIATION_LOCK_KEY = -1L;

    private final PaymentRepository paymentRepository;
    private final ReconciliationLogRepository reconciliationLogRepository;
    private final PaymentStateMachine stateMachine;
    private final PspStatusVerifier pspStatusVerifier;
    private final AdvisoryLockService advisoryLock;
    private final OutboxWriter outboxWriter;
    private final PaymentMetrics paymentMetrics;

    @Value("${payment.reconciliation.stuck-threshold-minutes:120}")
    private long stuckThresholdMinutes;

    @Override
    @Transactional
    public int reconcile() {
        if (!advisoryLock.tryLock(RECONCILIATION_LOCK_KEY)) {
            log.info("Reconciliation skipped — another run holds the lock.");
            return 0;
        }
        paymentMetrics.reconciliationRun();

        Instant cutoff = Instant.now().minus(Duration.ofMinutes(stuckThresholdMinutes));
        List<Payment> stuck = paymentRepository.findByStatusAndUpdatedAtBefore(PaymentStatus.PROCESSING, cutoff);
        log.info("Reconciliation found {} stuck payment(s) (PROCESSING before {})", stuck.size(), cutoff);

        int corrected = 0;
        for (Payment payment : stuck) {
            PaymentStatus verified = pspStatusVerifier.verify(payment);
            if (verified == null || verified == payment.getStatus()
                    || !stateMachine.canTransition(payment.getStatus(), verified)) {
                continue;
            }
            PaymentStatus old = payment.getStatus();
            stateMachine.transition(payment, verified);
            paymentRepository.save(payment);
            outboxWriter.appendForStatus(payment);
            if (verified == PaymentStatus.SUCCESS) {
                paymentMetrics.paymentSucceeded();
            } else if (verified == PaymentStatus.FAILED) {
                paymentMetrics.paymentFailed();
            }

            ReconciliationLog logEntry = new ReconciliationLog();
            logEntry.setPaymentId(payment.getId());
            logEntry.setOldStatus(old);
            logEntry.setNewStatus(verified);
            logEntry.setNote("Reconciled from PSP status query");
            reconciliationLogRepository.save(logEntry);

            log.info("Reconciled payment {}: {} -> {}", payment.getReference(), old, verified);
            corrected++;
        }
        return corrected;
    }
}
