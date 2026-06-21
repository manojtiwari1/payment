package com.app.modules.payment.service;

import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.enums.PaymentStatus;

/**
 * Queries a PSP for the authoritative status of a payment that has been stuck (e.g. PROCESSING
 * for too long). Simulated for the assignment.
 */
public interface PspStatusVerifier {

    /**
     * @return the PSP's reported terminal status for the payment, or {@code null} if still unknown.
     */
    PaymentStatus verify(Payment payment);
}
