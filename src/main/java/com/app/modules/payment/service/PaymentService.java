package com.app.modules.payment.service;

import com.app.infrastructure.security.userdetails.UserPrincipal;
import com.app.modules.payment.dto.CreatePaymentRequest;
import com.app.modules.payment.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    /**
     * Creates a payment idempotently. Returns the created payment (status PENDING), or the existing
     * payment if the {@code Idempotency-Key} was already used with the same payload.
     */
    PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey, UserPrincipal caller);

    /** Fetches one payment by reference, scoped to the caller's merchant (ADMIN sees all). */
    PaymentResponse getPayment(String reference, UserPrincipal caller);

    /** Lists payments — own merchant's for a MERCHANT, all for an ADMIN. */
    Page<PaymentResponse> listPayments(UserPrincipal caller, Pageable pageable);
}
