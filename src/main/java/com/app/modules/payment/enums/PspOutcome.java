package com.app.modules.payment.enums;

/**
 * Outcome a simulated PSP should produce for a charge (configured per provider).
 */
public enum PspOutcome {
    SUCCESS,
    TRANSIENT_FAILURE,
    TIMEOUT,
    SLOW
}
