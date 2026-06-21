package com.app.modules.payment.service.impl;

import com.app.common.enums.ResponseCode;
import com.app.common.exception.ApplicationException;
import com.app.infrastructure.security.userdetails.UserPrincipal;
import com.app.modules.payment.dto.CreatePaymentRequest;
import com.app.modules.payment.dto.PaymentResponse;
import com.app.modules.payment.entity.IdempotencyKey;
import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.exception.IdempotencyConflictException;
import com.app.modules.payment.repository.IdempotencyKeyRepository;
import com.app.modules.payment.repository.PaymentRepository;
import com.app.modules.payment.service.PaymentService;
import com.app.modules.payment.util.RequestHasher;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentCreationService paymentCreationService;
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final RateLimiterRegistry rateLimiterRegistry;

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey, UserPrincipal caller) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Idempotency-Key header is required.");
        }
        String key = idempotencyKey.trim();
        String merchantCode = resolveMerchantCode(caller, request);
        enforceRateLimit(merchantCode);
        String requestHash = RequestHasher.hash(merchantCode, request);

        try {
            Payment created = paymentCreationService.createNew(merchantCode, key, requestHash, request);
            log.info("Created payment {} for merchant {} (key={})", created.getReference(), merchantCode, key);
            return PaymentResponse.from(created);
        } catch (IdempotencyConflictException e) {
            return resolveExistingPayment(merchantCode, key, requestHash);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String reference, UserPrincipal caller) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new ApplicationException(ResponseCode.NOT_FOUND, "Payment not found."));
        authorizeRead(caller, payment);
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(UserPrincipal caller, Pageable pageable) {
        Page<Payment> page = isMerchant(caller)
                ? paymentRepository.findByMerchantCode(caller.getMerchantCode(), pageable)
                : paymentRepository.findAll(pageable);
        return page.map(PaymentResponse::from);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PaymentResponse resolveExistingPayment(String merchantCode, String key, String requestHash) {
        IdempotencyKey existing = idempotencyKeyRepository
                .findByMerchantCodeAndIdempotencyKey(merchantCode, key)
                .orElseThrow(() -> new ApplicationException(ResponseCode.FAILED,
                        "Idempotency record could not be resolved."));

        if (!existing.getRequestHash().equals(requestHash)) {
            throw new ApplicationException(ResponseCode.IDEMPOTENCY_CONFLICT,
                    "Idempotency-Key already used with a different request payload.");
        }
        Payment payment = paymentRepository.findById(existing.getPaymentId())
                .orElseThrow(() -> new ApplicationException(ResponseCode.NOT_FOUND, "Payment not found."));
        log.info("Idempotent replay for key={} → existing payment {}", key, payment.getReference());
        return PaymentResponse.from(payment);
    }

    /** MERCHANT callers are scoped to their own merchantCode; ADMIN callers must supply merchantId. */
    private String resolveMerchantCode(UserPrincipal caller, CreatePaymentRequest request) {
        if (isMerchant(caller)) {
            return caller.getMerchantCode();
        }
        String merchantId = request.getMerchantId();
        if (merchantId == null || merchantId.isBlank()) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST,
                    "merchantId is required for admin-initiated payments.");
        }
        return merchantId.trim();
    }

    private void authorizeRead(UserPrincipal caller, Payment payment) {
        if (isMerchant(caller) && !caller.getMerchantCode().equals(payment.getMerchantCode())) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED,
                    "You are not allowed to access this payment.");
        }
    }

    private boolean isMerchant(UserPrincipal caller) {
        return caller != null && caller.getMerchantCode() != null;
    }

    /** Per-merchant rate limit (default 100/min); rejects immediately with 429 when exceeded. */
    private void enforceRateLimit(String merchantCode) {
        RateLimiter limiter = rateLimiterRegistry.rateLimiter("merchant:" + merchantCode);
        if (!limiter.acquirePermission()) {
            throw new ApplicationException(ResponseCode.TOO_MANY_REQUESTS,
                    "Rate limit exceeded for merchant " + merchantCode + ". Please retry shortly.");
        }
    }
}

