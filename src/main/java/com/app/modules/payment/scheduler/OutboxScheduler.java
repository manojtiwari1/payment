package com.app.modules.payment.scheduler;

import com.app.modules.payment.service.OutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives the outbox relay on a fixed delay (default 5s; override with
 * {@code payment.outbox.poll-delay-ms}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRelay outboxRelay;

    @Scheduled(fixedDelayString = "${payment.outbox.poll-delay-ms:5000}")
    public void relay() {
        try {
            outboxRelay.publishPending();
        } catch (Exception e) {
            log.error("Outbox relay tick failed: {}", e.getMessage(), e);
        }
    }
}
