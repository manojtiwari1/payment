package com.app.common.enums;

public enum ResponseCode {

    ENTITY, DELETED, ACCESS_DENIED, NO_CONTENT,

    NOT_FOUND, FILE_SIZE_EXCEED, BAD_REQUEST, INVALID_PARAMETER,

    EMAIL_VERIFIED,

    INTERNAL_ERROR,

    DUPLICATE,

    CONFLICT,

    INVALID_REQUEST,

    ROLE_NOT_EXIST,

    FAILED,

    // ── Payment domain ────────────────────────────────────────────────────────
    IDEMPOTENCY_CONFLICT,

    INVALID_STATE_TRANSITION,

    PSP_UNAVAILABLE,

    TOO_MANY_REQUESTS,
}
