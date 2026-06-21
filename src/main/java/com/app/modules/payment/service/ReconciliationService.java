package com.app.modules.payment.service;

public interface ReconciliationService {

    /**
     * Reconciles payments stuck in PROCESSING against the PSP's authoritative status.
     *
     * @return number of payments corrected (0 if another run already holds the lock).
     */
    int reconcile();
}
