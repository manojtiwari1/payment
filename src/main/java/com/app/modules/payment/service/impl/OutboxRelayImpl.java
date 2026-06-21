package com.app.modules.payment.service.impl;

import com.app.modules.payment.entity.OutboxEvent;
import com.app.modules.payment.repository.OutboxEventRepository;
import com.app.modules.payment.service.OutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relays unpublished outbox rows to Kafka. Sends in id order; if a send fails (e.g. broker down) it
 * stops the pass, leaving that row and the rest unpublished to be retried on the next tick — so no
 * event is lost. Delivery is at-least-once; consumers dedupe on {@code eventId}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayImpl implements OutboxRelay {

    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${payment.outbox.topic:payment-events}")
    private String topic;

    @Override
    @Transactional
    public int publishPending() {
        List<OutboxEvent> batch = outboxEventRepository
                .findByPublishedFalseOrderByIdAsc(Limit.of(BATCH_SIZE));
        if (batch.isEmpty()) {
            return 0;
        }

        int published = 0;
        for (OutboxEvent event : batch) {
            try {
                // Block briefly for the broker ack so we only mark published on confirmed delivery.
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Outbox publish failed at id={} ({}); will retry next tick.",
                        event.getId(), e.getMessage());
                break; // stop the pass; preserve ordering and back off the broker
            }
            event.setPublished(true);
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
            published++;
        }

        if (published > 0) {
            log.info("Outbox relay published {} event(s) to topic '{}'.", published, topic);
        }
        return published;
    }
}
