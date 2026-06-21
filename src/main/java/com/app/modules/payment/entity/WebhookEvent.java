package com.app.modules.payment.entity;

import com.app.common.model.AbstractAuditableModel;
import com.app.modules.payment.enums.Psp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Stored PSP webhook. The {@code UNIQUE(event_id)} constraint makes processing idempotent —
 * a repeated delivery of the same {@code eventId} is recognised and ignored.
 */
@Getter
@Setter
@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_event_id", columnNames = "event_id"),
        indexes = @Index(name = "idx_webhook_payment", columnList = "payment_id")
)
public class WebhookEvent extends AbstractAuditableModel {

    @Column(name = "event_id", nullable = false, length = 200)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Psp psp;

    /** Internal payment id, resolved from the webhook's payment reference (null if unknown). */
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private boolean processed;
}
