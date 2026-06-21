package com.app.modules.payment.controller;

import com.app.common.enums.ResponseCode;
import com.app.common.exception.ApplicationException;
import com.app.common.response.BaseResponse;
import com.app.common.response.Response;
import com.app.modules.payment.dto.WebhookRequest;
import com.app.modules.payment.enums.Psp;
import com.app.modules.payment.enums.WebhookOutcome;
import com.app.modules.payment.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Receives asynchronous PSP webhooks. Public (PSPs have no JWT); idempotency and out-of-order
 * protection are handled in the webhook service.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController extends BaseResponse {

    private final WebhookService webhookService;

    @PostMapping("/{psp}")
    public ResponseEntity<Response> receive(@PathVariable String psp,
                                            @Valid @RequestBody WebhookRequest request) {
        Psp provider = parsePsp(psp);
        WebhookOutcome outcome = webhookService.process(provider, request);
        return data(Map.of("eventId", request.getEventId(), "result", outcome.name()));
    }

    private Psp parsePsp(String psp) {
        try {
            return Psp.valueOf(psp.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Unknown PSP: " + psp);
        }
    }
}
