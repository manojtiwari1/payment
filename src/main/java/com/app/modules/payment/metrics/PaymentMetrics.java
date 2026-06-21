package com.app.modules.payment.metrics;

import com.app.modules.payment.enums.WebhookOutcome;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer counters for the payment domain, exposed via Spring Boot Actuator
 * ({@code /actuator/metrics/<name>}):
 * {@code payment_success_total}, {@code payment_failure_total},
 * {@code webhook_processed_total} (tagged by {@code result}), {@code reconciliation_runs_total}.
 */
@Component
public class PaymentMetrics {

    private final MeterRegistry registry;
    private final Counter paymentSuccess;
    private final Counter paymentFailure;
    private final Counter reconciliationRuns;

    public PaymentMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.paymentSuccess = Counter.builder("payment_success_total")
                .description("Payments that reached SUCCESS").register(registry);
        this.paymentFailure = Counter.builder("payment_failure_total")
                .description("Payments that reached FAILED").register(registry);
        this.reconciliationRuns = Counter.builder("reconciliation_runs_total")
                .description("Reconciliation passes that executed").register(registry);
    }

    public void paymentSucceeded() {
        paymentSuccess.increment();
    }

    public void paymentFailed() {
        paymentFailure.increment();
    }

    public void reconciliationRun() {
        reconciliationRuns.increment();
    }

    public void webhookProcessed(WebhookOutcome outcome) {
        Counter.builder("webhook_processed_total")
                .description("Webhook deliveries processed")
                .tag("result", outcome.name())
                .register(registry)
                .increment();
    }
}
