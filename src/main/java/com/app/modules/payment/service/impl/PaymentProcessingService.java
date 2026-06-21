package com.app.modules.payment.service.impl;

import com.app.common.enums.ResponseCode;
import com.app.common.exception.ApplicationException;
import com.app.modules.payment.dto.ChargeContext;
import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.entity.PaymentAttempt;
import com.app.modules.payment.enums.AttemptStatus;
import com.app.modules.payment.enums.PaymentStatus;
import com.app.modules.payment.enums.Psp;
import com.app.modules.payment.metrics.PaymentMetrics;
import com.app.modules.payment.repository.PaymentAttemptRepository;
import com.app.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The transactional units of the routing flow, kept in a dedicated bean so each runs in its own
 * short transaction (and Spring's proxy applies — no self-invocation). The routing engine
 * orchestrates these around the non-transactional PSP calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentStateMachine stateMachine;
    private final OutboxWriter outboxWriter;
    private final PaymentMetrics paymentMetrics;

    /**
     * Moves a PENDING payment to PROCESSING. Returns a detached {@link ChargeContext}, or
     * {@code null} if the payment is not in a routable state (already processed / not found).
     */
    @Transactional
    public ChargeContext beginProcessing(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Routing requested for unknown payment id={}", paymentId);
            return null;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.debug("Payment {} not PENDING (is {}), skipping routing", paymentId, payment.getStatus());
            return null;
        }
        stateMachine.transition(payment, PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        outboxWriter.appendForStatus(payment);
        return new ChargeContext(payment.getId(), payment.getReference(), payment.getMerchantCode(),
                payment.getCustomerId(), payment.getAmount(), payment.getCurrency());
    }

    @Transactional
    public Long startAttempt(Long paymentId, Psp psp) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setPaymentId(paymentId);
        attempt.setPsp(psp);
        attempt.setStatus(AttemptStatus.STARTED);
        attempt.setStartedAt(Instant.now());
        return attemptRepository.save(attempt).getId();
    }

    @Transactional
    public void succeed(Long attemptId, Long paymentId, Psp psp) {
        PaymentAttempt attempt = requireAttempt(attemptId);
        attempt.setStatus(AttemptStatus.SUCCESS);
        attempt.setCompletedAt(Instant.now());
        attemptRepository.save(attempt);

        Payment payment = requirePayment(paymentId);
        stateMachine.transition(payment, PaymentStatus.SUCCESS);
        payment.setSelectedPsp(psp);
        paymentRepository.save(payment);
        outboxWriter.appendForStatus(payment);
        paymentMetrics.paymentSucceeded();
        log.info("Payment {} SUCCESS via {}", payment.getReference(), psp);
    }

    @Transactional
    public void failAttempt(Long attemptId, String errorMessage) {
        PaymentAttempt attempt = requireAttempt(attemptId);
        attempt.setStatus(AttemptStatus.FAILED);
        attempt.setErrorMessage(truncate(errorMessage));
        attempt.setCompletedAt(Instant.now());
        attemptRepository.save(attempt);
    }

    /**
     * Records an ambiguous attempt (e.g. timeout) without changing the payment, which stays in
     * PROCESSING so reconciliation can determine the real outcome — avoiding a failover double-charge.
     */
    @Transactional
    public void markInconclusive(Long attemptId, String message) {
        PaymentAttempt attempt = requireAttempt(attemptId);
        attempt.setStatus(AttemptStatus.INDETERMINATE);
        attempt.setErrorMessage(truncate(message));
        attempt.setCompletedAt(Instant.now());
        attemptRepository.save(attempt);
    }

    @Transactional
    public void markFailed(Long paymentId) {
        Payment payment = requirePayment(paymentId);
        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            stateMachine.transition(payment, PaymentStatus.FAILED);
            paymentRepository.save(payment);
            outboxWriter.appendForStatus(payment);
            paymentMetrics.paymentFailed();
            log.info("Payment {} FAILED — all PSPs exhausted", payment.getReference());
        }
    }

    private PaymentAttempt requireAttempt(Long id) {
        return attemptRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ResponseCode.NOT_FOUND, "Attempt not found."));
    }

    private Payment requirePayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ResponseCode.NOT_FOUND, "Payment not found."));
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
