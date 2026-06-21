package com.app.modules.payment.entity;

import com.app.common.model.AbstractAuditableModel;
import com.app.modules.payment.enums.PaymentStatus;
import com.app.modules.payment.enums.Psp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A payment aggregate. Postgres is the source of truth; {@link Version} enables optimistic
 * locking so concurrent webhook/reconciliation/routing updates cannot silently overwrite.
 *
 * <p>{@code reference} is the externally-exposed payment id (e.g. "PAY-ab12cd34"); the numeric
 * inherited {@code id} is used internally for foreign keys.
 */
@Getter
@Setter
@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payments_reference", columnNames = "reference"),
        indexes = {
                @Index(name = "idx_payments_reference", columnList = "reference"),
                @Index(name = "idx_payments_merchant", columnList = "merchant_code"),
                @Index(name = "idx_payments_status", columnList = "status")
        }
)
public class Payment extends AbstractAuditableModel {

    @Column(nullable = false, updatable = false, length = 40)
    private String reference;

    @Column(name = "merchant_code", nullable = false)
    private String merchantCode;

    @Column(name = "customer_id")
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_psp", length = 20)
    private Psp selectedPsp;

    @Version
    private Long version;
}
