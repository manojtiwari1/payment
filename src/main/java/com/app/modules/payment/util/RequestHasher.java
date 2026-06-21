package com.app.modules.payment.util;

import com.app.modules.payment.dto.CreatePaymentRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Produces a stable SHA-256 hash of the business-significant fields of a payment request, used to
 * detect reuse of an idempotency key with a different payload (→ 409).
 */
public final class RequestHasher {

    private RequestHasher() {
    }

    public static String hash(String merchantCode, CreatePaymentRequest request) {
        String amount = request.getAmount() == null
                ? "" : request.getAmount().stripTrailingZeros().toPlainString();
        String currency = request.getCurrency() == null ? "" : request.getCurrency().toUpperCase();
        String customerId = request.getCustomerId() == null ? "" : request.getCustomerId();

        String canonical = String.join("|", merchantCode, amount, currency, customerId);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
