package com.app.modules.payment.enums;

/**
 * Lifecycle states of a payment. Transitions are enforced by the payment state machine.
 */
public enum PaymentStatus {
    CREATED,
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED
}
