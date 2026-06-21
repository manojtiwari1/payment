package com.app.modules.payment.service;

import com.app.modules.payment.dto.WebhookRequest;
import com.app.modules.payment.enums.Psp;
import com.app.modules.payment.enums.WebhookOutcome;

public interface WebhookService {

    /**
     * Processes a PSP webhook idempotently and out-of-order-safely.
     *
     * @return whether the event was applied, was a duplicate, or referenced an unknown payment.
     */
    WebhookOutcome process(Psp psp, WebhookRequest request);
}
