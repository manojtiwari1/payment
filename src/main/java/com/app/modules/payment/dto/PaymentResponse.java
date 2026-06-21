package com.app.modules.payment.dto;

import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.enums.PaymentStatus;
import com.app.modules.payment.enums.Psp;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * API view of a payment. {@code paymentId} is the external reference (e.g. "PAY-ab12cd34").
 */
@Value
@Builder
public class PaymentResponse {

    String paymentId;
    PaymentStatus status;
    String merchantId;
    String customerId;
    BigDecimal amount;
    String currency;
    Psp selectedPsp;
    Instant createdAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getReference())
                .status(payment.getStatus())
                .merchantId(payment.getMerchantCode())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .selectedPsp(payment.getSelectedPsp())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
