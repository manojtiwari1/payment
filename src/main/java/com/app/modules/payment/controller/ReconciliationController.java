package com.app.modules.payment.controller;

import com.app.common.response.BaseResponse;
import com.app.common.response.Response;
import com.app.modules.payment.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Operational endpoint (ADMIN only) to trigger reconciliation on demand, in addition to the
 * scheduled run.
 */
@RestController
@RequestMapping("/admin/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController extends BaseResponse {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> run() {
        int corrected = reconciliationService.reconcile();
        return data(Map.of("corrected", corrected));
    }
}
