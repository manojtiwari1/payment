package com.app.modules.payment.service.impl;

import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.enums.PaymentStatus;
import com.app.modules.payment.service.PspStatusVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simulated PSP status verification. By default it reports stuck payments as SUCCESS (assuming the
 * PSP completed the charge but the webhook was lost), configurable via
 * {@code payment.reconciliation.assumed-status}.
 */
@Component
public class SimulatedPspStatusVerifier implements PspStatusVerifier {

    private final PaymentStatus assumedStatus;

    public SimulatedPspStatusVerifier(
            @Value("${payment.reconciliation.assumed-status:SUCCESS}") String assumedStatus) {
        this.assumedStatus = PaymentStatus.valueOf(assumedStatus.trim().toUpperCase());
    }

    @Override
    public PaymentStatus verify(Payment payment) {
        return assumedStatus;
    }
}
