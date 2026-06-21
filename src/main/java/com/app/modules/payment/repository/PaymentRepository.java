package com.app.modules.payment.repository;

import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(String reference);

    Page<Payment> findByMerchantCode(String merchantCode, Pageable pageable);

    /** Payments left in {@code status} and untouched since {@code cutoff} — candidates for reconciliation. */
    List<Payment> findByStatusAndUpdatedAtBefore(PaymentStatus status, Instant cutoff);
}
