package com.app.modules.payment.controller;

import com.app.common.response.BaseResponse;
import com.app.common.response.Response;
import com.app.infrastructure.security.userdetails.UserPrincipal;
import com.app.modules.payment.dto.CreatePaymentRequest;
import com.app.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Merchant-facing payment API. Creation is idempotent via the {@code Idempotency-Key} header;
 * routing runs asynchronously after the create transaction commits, so the response is {@code PENDING}.
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController extends BaseResponse {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<Response> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        return data(paymentService.createPayment(request, idempotencyKey, principal));
    }

    @GetMapping("/{reference}")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<Response> getOne(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String reference) {
        return data(paymentService.getPayment(reference, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<Response> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Sort sort = Sort.by("createdAt").descending();
        return data(paymentService.listPayments(principal, PageRequest.of(page, size, sort)));
    }
}
