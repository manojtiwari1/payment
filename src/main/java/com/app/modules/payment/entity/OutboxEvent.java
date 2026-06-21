package com.app.modules.payment.entity;

import com.app.common.model.AbstractAuditableModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Transactional outbox row. Written in the <em>same transaction</em> as the payment state change it
 * describes, then relayed to Kafka by a separate poller. Guarantees no event is lost if the broker
 * is unavailable (at-least-once delivery; consumers dedupe on {@code event_id}).
 */
@Getter
@Setter
@Entity
@Table(
        name = "outbox_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event_id", columnNames = "event_id"),
        indexes = @Index(name = "idx_outbox_published", columnList = "published, id")
)
public class OutboxEvent extends AbstractAuditableModel {

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    /** The aggregate this event belongs to (the payment reference). */
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;
}
