package com.app.modules.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * PSP webhook payload, e.g. {@code {"eventId":"EV123","paymentId":"PAY-...","status":"SUCCESS"}}.
 * {@code paymentId} is the external payment reference.
 */
@Data
public class WebhookRequest {

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "paymentId is required")
    private String paymentId;

    @NotBlank(message = "status is required")
    private String status;
}
