package com.app.modules.payment.entity;

import com.app.common.model.AbstractAuditableModel;
import com.app.modules.payment.enums.AttemptStatus;
import com.app.modules.payment.enums.Psp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Audit record of one PSP attempt for a payment, enabling full routing traceability
 * (e.g. PSP_A FAILED → PSP_B SUCCESS).
 */
@Getter
@Setter
@Entity
@Table(
        name = "payment_attempts",
        indexes = @Index(name = "idx_payment_attempts_payment", columnList = "payment_id")
)
public class PaymentAttempt extends AbstractAuditableModel {

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Psp psp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
