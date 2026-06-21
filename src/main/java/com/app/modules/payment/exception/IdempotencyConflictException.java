package com.app.modules.payment.exception;

/**
 * Internal marker thrown when a concurrent/duplicate request lost the race on the
 * {@code (merchant_code, idempotency_key)} unique index. Being a {@link RuntimeException}, it rolls
 * back the losing request's payment insert; the orchestrator then returns the winner's payment.
 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("Idempotency key already exists");
    }
}
