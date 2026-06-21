package com.app.modules.payment.entity;

import com.app.common.model.AbstractAuditableModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Records a processed idempotency request. The {@code (merchant_code, idempotency_key)} unique
 * constraint is the sole concurrency control guaranteeing one payment per key, even under
 * concurrent requests in a distributed deployment.
 */
@Getter
@Setter
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotency_merchant_key",
                columnNames = {"merchant_code", "idempotency_key"}),
        indexes = @Index(name = "idx_idempotency_merchant_key", columnList = "merchant_code, idempotency_key")
)
public class IdempotencyKey extends AbstractAuditableModel {

    @Column(name = "merchant_code", nullable = false)
    private String merchantCode;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
}
