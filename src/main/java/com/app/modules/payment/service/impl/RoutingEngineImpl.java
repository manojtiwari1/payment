package com.app.modules.payment.service.impl;

import com.app.modules.payment.config.RoutingProperties;
import com.app.modules.payment.dto.ChargeContext;
import com.app.modules.payment.dto.PspChargeRequest;
import com.app.modules.payment.dto.PspResult;
import com.app.modules.payment.enums.Psp;
import com.app.modules.payment.service.PspGateway;
import com.app.modules.payment.service.RoutingEngine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrates PSP routing with failover, guarded by a per-PSP Resilience4j circuit breaker. Each DB
 * mutation happens in its own short transaction (via {@link PaymentProcessingService}); the PSP calls
 * run outside any transaction.
 *
 * <p>Flow: PENDING → PROCESSING, then try each configured PSP in order — first success ends the flow
 * (SUCCESS); a definitive failure (or an open breaker) fails over; an indeterminate result (e.g.
 * timeout) leaves the payment PROCESSING for reconciliation; if all fail, status FAILED.
 *
 * <p>The breaker counts business failures and timeouts (not just thrown exceptions): when a PSP's
 * failure rate trips it, that PSP is skipped (permission denied) and routing fails over immediately.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingEngineImpl implements RoutingEngine {

    private final PspGateway pspGateway;
    private final RoutingProperties routingProperties;
    private final PaymentProcessingService processing;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public void route(Long paymentId) {
        ChargeContext ctx = processing.beginProcessing(paymentId);
        if (ctx == null) {
            return;
        }

        List<Psp> order = routingProperties.resolve(ctx.merchantCode());
        if (order.isEmpty()) {
            log.warn("No PSP route configured for merchant {} — marking payment {} FAILED",
                    ctx.merchantCode(), ctx.reference());
            processing.markFailed(paymentId);
            return;
        }

        PspChargeRequest request = new PspChargeRequest(
                ctx.paymentId(), ctx.reference(), ctx.merchantCode(),
                ctx.customerId(), ctx.amount(), ctx.currency());

        for (Psp psp : order) {
            Long attemptId = processing.startAttempt(paymentId, psp);
            CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(psp.name());

            if (!breaker.tryAcquirePermission()) {
                log.warn("Circuit OPEN for {} on payment {}; skipping and failing over", psp, ctx.reference());
                processing.failAttempt(attemptId, "CIRCUIT_OPEN: breaker open for " + psp);
                continue;
            }

            long start = System.nanoTime();
            PspResult result;
            try {
                result = pspGateway.charge(psp, request);
            } catch (Exception e) {
                breaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, e);
                log.warn("PSP {} threw for payment {}: {}", psp, ctx.reference(), e.getMessage());
                processing.failAttempt(attemptId, e.getMessage());
                continue;
            }
            long elapsed = System.nanoTime() - start;

            if (result.isSuccess()) {
                breaker.onSuccess(elapsed, TimeUnit.NANOSECONDS);
                processing.succeed(attemptId, paymentId, psp);
                return;
            }
            if (result.isIndeterminate()) {
                // Timeout/ambiguous: count it against the breaker, but do NOT fail over (double-charge
                // risk). Leave the payment in PROCESSING for reconciliation to resolve.
                breaker.onError(elapsed, TimeUnit.NANOSECONDS, new TimeoutException(result.message()));
                log.warn("PSP {} indeterminate for payment {} ({}); leaving PROCESSING for reconciliation",
                        psp, ctx.reference(), result.code());
                processing.markInconclusive(attemptId, result.code() + ": " + result.message());
                return;
            }
            // Definitive failure: count against the breaker and fail over.
            breaker.onError(elapsed, TimeUnit.NANOSECONDS, new IllegalStateException(result.message()));
            log.info("PSP {} failed for payment {} ({}); failing over", psp, ctx.reference(), result.code());
            processing.failAttempt(attemptId, result.code() + ": " + result.message());
        }

        processing.markFailed(paymentId);
    }
}
