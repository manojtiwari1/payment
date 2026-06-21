package com.app.modules.payment.enums;

/**
 * Result of processing a webhook delivery.
 */
public enum WebhookOutcome {
    /** First delivery; applied (or acknowledged) successfully. */
    PROCESSED,
    /** Duplicate delivery of an already-seen eventId; ignored. */
    DUPLICATE,
    /** Event referenced a payment that does not exist; recorded but not applied. */
    UNKNOWN_PAYMENT
}
