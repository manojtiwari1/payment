package com.app.modules.payment.enums;

/**
 * Outcome of a single PSP routing attempt.
 */
public enum AttemptStatus {
    STARTED,
    SUCCESS,
    FAILED,
    /** Ambiguous outcome (e.g. PSP timeout) — no failover; left for reconciliation. */
    INDETERMINATE
}
