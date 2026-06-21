package com.app.modules.payment.event;

/**
 * Internal (in-process) Spring application event published when a payment is persisted, used to
 * trigger asynchronous routing after the creating transaction commits. Distinct from the domain
 * events published to Kafka via the outbox (added in a later phase).
 */
public record PaymentCreatedInternalEvent(Long paymentId) {
}
