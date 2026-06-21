package com.app.modules.payment.event;

import com.app.modules.payment.service.RoutingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Triggers payment routing only after the creating transaction has committed, on a background
 * thread — so {@code POST /payments} can return {@code PENDING} immediately while the PSP routing
 * proceeds asynchronously.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRoutingListener {

    private final RoutingEngine routingEngine;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCreated(PaymentCreatedInternalEvent event) {
        try {
            routingEngine.route(event.paymentId());
        } catch (Exception e) {
            log.error("Routing failed for payment id={}: {}", event.paymentId(), e.getMessage(), e);
        }
    }
}
