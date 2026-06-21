package com.app.infrastructure.security.jwt;

/**
 * Distinguishes the two self-contained JWTs the application issues.
 * Stamped into the {@code type} claim so the refresh endpoint can reject access
 * tokens (and protected resources can reject refresh tokens).
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
