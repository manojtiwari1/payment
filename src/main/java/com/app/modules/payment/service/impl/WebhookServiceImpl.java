package com.app.modules.payment.service.impl;

import com.app.common.enums.ResponseCode;
import com.app.common.exception.ApplicationException;
import com.app.infrastructure.lock.AdvisoryLockService;
import com.app.modules.payment.dto.WebhookRequest;
import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.enums.PaymentStatus;
import com.app.modules.payment.enums.Psp;
import com.app.modules.payment.enums.WebhookOutcome;
import com.app.modules.payment.metrics.PaymentMetrics;
import com.app.modules.payment.repository.PaymentRepository;
import com.app.modules.payment.repository.WebhookEventRepository;
import com.app.modules.payment.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes PSP webhooks idempotently ({@code UNIQUE(event_id)}), out-of-order-safely (state machine
 * gates the transition), and race-safely (a transaction-scoped advisory lock on the payment,
 * acquired before reading so serialised deliveries see fresh state).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookEventRepository webhookRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;

    private final AdvisoryLockService advisoryLock;

    private final OutboxWriter outboxWriter;

    private final PaymentMetrics paymentMetrics;

    @Override
    @Transactional
    public WebhookOutcome process(Psp psp, WebhookRequest request) {
        PaymentStatus target = parseStatus(request.getStatus());

        // Lock by payment reference BEFORE reading, so serialised deliveries always see fresh state
        // (released at commit). Keyed by the reference hash since the internal id isn't known yet.
        advisoryLock.lock(lockKeyFor(request.getPaymentId()));

        Payment payment = paymentRepository.findByReference(request.getPaymentId()).orElse(null);
        Long paymentId = payment != null ? payment.getId() : null;

        int inserted = webhookRepository.insertIfAbsent(
                request.getEventId(), psp.name(), paymentId, toPayload(psp, request));
        if (inserted == 0) {
            log.info("Duplicate webhook eventId={} ignored", request.getEventId());
            paymentMetrics.webhookProcessed(WebhookOutcome.DUPLICATE);
            return WebhookOutcome.DUPLICATE;
        }

        if (payment == null) {
            log.warn("Webhook eventId={} references unknown payment {}", request.getEventId(), request.getPaymentId());
            webhookRepository.markProcessed(request.getEventId());
            paymentMetrics.webhookProcessed(WebhookOutcome.UNKNOWN_PAYMENT);
            return WebhookOutcome.UNKNOWN_PAYMENT;
        }

        if (stateMachine.canTransition(payment.getStatus(), target)) {
            PaymentStatus from = payment.getStatus();
            stateMachine.transition(payment, target);
            if (target == PaymentStatus.SUCCESS && payment.getSelectedPsp() == null) {
                payment.setSelectedPsp(psp);
            }
            paymentRepository.save(payment);
            outboxWriter.appendForStatus(payment);
            recordTerminalMetric(target);
            log.info("Webhook {} applied: payment {} {} -> {}", request.getEventId(),
                    payment.getReference(), from, target);
        } else {
            log.info("Webhook {} ignored (out-of-order/illegal {} -> {}) for payment {}",
                    request.getEventId(), payment.getStatus(), target, payment.getReference());
        }

        webhookRepository.markProcessed(request.getEventId());
        paymentMetrics.webhookProcessed(WebhookOutcome.PROCESSED);
        return WebhookOutcome.PROCESSED;
    }

    private void recordTerminalMetric(PaymentStatus target) {
        if (target == PaymentStatus.SUCCESS) {
            paymentMetrics.paymentSucceeded();
        } else if (target == PaymentStatus.FAILED) {
            paymentMetrics.paymentFailed();
        }
    }

    /** Stable 64-bit advisory-lock key for a payment reference (collisions only over-serialise). */
    private long lockKeyFor(String reference) {
        long h = 1125899906842597L; // prime
        for (int i = 0; i < reference.length(); i++) {
            h = 31 * h + reference.charAt(i);
        }
        return h;
    }

    private PaymentStatus parseStatus(String status) {
        try {
            return PaymentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Unknown webhook status: " + status);
        }
    }

    private String toPayload(Psp psp, WebhookRequest r) {
        return "{"
                + "\"eventId\":\"" + escape(r.getEventId()) + "\","
                + "\"paymentId\":\"" + escape(r.getPaymentId()) + "\","
                + "\"status\":\"" + escape(r.getStatus()) + "\","
                + "\"psp\":\"" + psp.name() + "\""
                + "}";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
