package com.app.modules.payment.enums;

/**
 * Domain event types published through the outbox.
 */
public enum OutboxEventType {
    PAYMENT_CREATED,
    PAYMENT_PROCESSING,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED
}
