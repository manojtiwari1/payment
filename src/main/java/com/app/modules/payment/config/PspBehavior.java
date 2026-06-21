package com.app.modules.payment.config;

import com.app.modules.payment.enums.PspOutcome;

/**
 * Configured simulated behavior for a single PSP (bound under {@code payment.psp.providers.<PSP>}).
 */
public class PspBehavior {

    private PspOutcome outcome = PspOutcome.SUCCESS;

    private long latencyMs = 0;

    public PspOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(PspOutcome outcome) {
        this.outcome = outcome;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
