package com.app.modules.payment.dto;

import java.math.BigDecimal;

/**
 * Minimal, detached snapshot of a payment needed to drive routing, so the engine never holds a
 * JPA entity (or transaction) across a PSP call.
 */
public record ChargeContext(
        Long paymentId,
        String reference,
        String merchantCode,
        String customerId,
        BigDecimal amount,
        String currency) {
}
