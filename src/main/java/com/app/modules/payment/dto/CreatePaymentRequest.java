package com.app.modules.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payment creation payload. {@code merchantId} is honored only for ADMIN callers; for MERCHANT
 * callers the merchant is derived from the authenticated principal and this field is ignored.
 */
@Data
public class CreatePaymentRequest {

    /** Optional; used only by ADMIN callers. MERCHANT callers are scoped to their own merchant. */
    private String merchantId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;

    private String customerId;
}
