package com.app.modules.payment.entity;

import com.app.common.model.AbstractAuditableModel;
import com.app.modules.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Audit record of a reconciliation action that corrected a stuck payment's status.
 */
@Getter
@Setter
@Entity
@Table(
        name = "reconciliation_logs",
        indexes = @Index(name = "idx_reconciliation_payment", columnList = "payment_id")
)
public class ReconciliationLog extends AbstractAuditableModel {

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private PaymentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private PaymentStatus newStatus;

    @Column(length = 500)
    private String note;
}
