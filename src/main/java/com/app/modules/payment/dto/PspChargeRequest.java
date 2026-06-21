package com.app.modules.payment.dto;

import java.math.BigDecimal;

/**
 * Immutable charge instruction handed to a PSP. Carries only what a provider needs — no JPA
 * entity is passed, so no transaction is held across the (simulated) network call.
 */
public record PspChargeRequest(
        Long paymentId,
        String reference,
        String merchantCode,
        String customerId,
        BigDecimal amount,
        String currency) {
}
