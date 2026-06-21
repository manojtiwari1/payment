package com.app.modules.payment.service.impl;

import com.app.modules.payment.config.PspBehavior;
import com.app.modules.payment.config.PspSimulationProperties;
import com.app.modules.payment.dto.PspChargeRequest;
import com.app.modules.payment.dto.PspResult;
import com.app.modules.payment.enums.Psp;
import com.app.modules.payment.service.PspGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simulates PSP behavior from configuration ({@link PspSimulationProperties}): success, transient
 * failure, timeout (treated as indeterminate), or a slow-but-successful response.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulatedPspGateway implements PspGateway {

    private final PspSimulationProperties properties;

    @Override
    public PspResult charge(Psp psp, PspChargeRequest request) {
        PspBehavior behavior = properties.behaviorFor(psp);
        log.info("PSP {} charging payment={} amount={} {} (simulated outcome={})",
                psp, request.reference(), request.amount(), request.currency(), behavior.getOutcome());

        sleepQuietly(behavior.getLatencyMs());

        return switch (behavior.getOutcome()) {
            case SUCCESS -> PspResult.success();
            case SLOW -> {
                sleepQuietly(Math.max(behavior.getLatencyMs(), 200));
                yield PspResult.success();
            }
            case TRANSIENT_FAILURE ->
                    PspResult.failure("PSP_TRANSIENT", psp + " returned a temporary failure");
            // A timeout is ambiguous — the charge may have succeeded. Treat as indeterminate so
            // routing does not fail over (which could double-charge); reconciliation resolves it.
            case TIMEOUT ->
                    PspResult.indeterminate("PSP_TIMEOUT", psp + " timed out");
        };
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
