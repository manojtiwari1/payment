package com.app.modules.payment.enums;

/**
 * Status of a single PSP charge call.
 *
 * <ul>
 *   <li>{@code SUCCESS} — charge approved.</li>
 *   <li>{@code FAILED} — definitively declined; safe to fail over to the next PSP.</li>
 *   <li>{@code INDETERMINATE} — ambiguous (e.g. timeout): failing over risks a double charge, so
 *       routing stops and leaves the payment in PROCESSING for reconciliation.</li>
 * </ul>
 */
public enum PspResultStatus {
    SUCCESS,
    FAILED,
    INDETERMINATE
}
