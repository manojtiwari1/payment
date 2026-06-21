package com.app.modules.payment.dto;

import com.app.modules.payment.enums.PspResultStatus;

/**
 * Outcome of a single PSP charge call. See {@link PspResultStatus} for the meaning of each status.
 */
public record PspResult(PspResultStatus status, String code, String message) {

    public static PspResult success() {
        return new PspResult(PspResultStatus.SUCCESS, "OK", "Charge approved");
    }

    public static PspResult failure(String code, String message) {
        return new PspResult(PspResultStatus.FAILED, code, message);
    }

    public static PspResult indeterminate(String code, String message) {
        return new PspResult(PspResultStatus.INDETERMINATE, code, message);
    }

    public boolean isSuccess() {
        return status == PspResultStatus.SUCCESS;
    }

    public boolean isIndeterminate() {
        return status == PspResultStatus.INDETERMINATE;
    }
}
