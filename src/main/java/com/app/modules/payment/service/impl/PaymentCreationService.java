package com.app.modules.payment.service.impl;

import com.app.infrastructure.messaging.EventPublisher;
import com.app.modules.payment.dto.CreatePaymentRequest;
import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.enums.OutboxEventType;
import com.app.modules.payment.enums.PaymentStatus;
import com.app.modules.payment.event.PaymentCreatedInternalEvent;
import com.app.modules.payment.exception.IdempotencyConflictException;
import com.app.modules.payment.repository.IdempotencyKeyRepository;
import com.app.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Atomically creates a payment together with its idempotency record. Kept in its own bean so the
 * {@link Transactional} boundary is real (no self-invocation) and so a conflict rolls back the
 * payment insert too.
 */
@Service
@RequiredArgsConstructor
public class PaymentCreationService {

    private final PaymentRepository paymentRepository;

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    private final EventPublisher eventPublisher;

    private final OutboxWriter outboxWriter;

    /**
     * Inserts the payment (PENDING) and the idempotency key in one transaction. If the key already
     * exists, throws {@link IdempotencyConflictException} so the whole transaction (including the
     * payment row) rolls back. On success, publishes an after-commit routing trigger.
     */
    @Transactional
    public Payment createNew(String merchantCode, String idempotencyKey,
                             String requestHash, CreatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setReference(generateReference());
        payment.setMerchantCode(merchantCode);
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment); // IDENTITY → id assigned immediately

        int inserted = idempotencyKeyRepository.insertIfAbsent(
                merchantCode, idempotencyKey, payment.getId(), requestHash);
        if (inserted == 0) {
            throw new IdempotencyConflictException();
        }

        // Domain event in the SAME transaction as the payment insert (transactional outbox).
        outboxWriter.append(payment, OutboxEventType.PAYMENT_CREATED);

        // AFTER_COMMIT listener kicks off routing once this transaction commits.
        eventPublisher.publish(new PaymentCreatedInternalEvent(payment.getId()));
        return payment;
    }

    private String generateReference() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
