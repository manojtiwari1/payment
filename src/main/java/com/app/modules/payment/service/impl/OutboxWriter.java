package com.app.modules.payment.service.impl;

import com.app.modules.payment.entity.OutboxEvent;
import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.enums.OutboxEventType;
import com.app.modules.payment.enums.PaymentStatus;
import com.app.modules.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Appends domain events to the outbox <em>within the caller's transaction</em>
 * ({@link Propagation#MANDATORY} guarantees there is one) — so an event is persisted atomically with
 * the payment state change it describes. The {@code OutboxRelay} ships them to Kafka asynchronously.
 */
@Service
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(Payment payment, OutboxEventType type) {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setAggregateId(payment.getReference());
        event.setEventType(type.name());
        event.setPayload(buildPayload(eventId, payment));
        event.setPublished(false);
        outboxEventRepository.save(event);
    }

    /** Emits the event matching the payment's current status (no-op for statuses without an event). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendForStatus(Payment payment) {
        OutboxEventType type = forStatus(payment.getStatus());
        if (type != null) {
            append(payment, type);
        }
    }

    private OutboxEventType forStatus(PaymentStatus status) {
        return switch (status) {
            case PROCESSING -> OutboxEventType.PAYMENT_PROCESSING;
            case SUCCESS -> OutboxEventType.PAYMENT_SUCCEEDED;
            case FAILED -> OutboxEventType.PAYMENT_FAILED;
            default -> null;
        };
    }

    private String buildPayload(String eventId, Payment payment) {
        return "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"paymentId\":\"" + escape(payment.getReference()) + "\","
                + "\"merchantId\":\"" + escape(payment.getMerchantCode()) + "\","
                + "\"status\":\"" + payment.getStatus().name() + "\","
                + "\"timestamp\":\"" + Instant.now() + "\""
                + "}";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
