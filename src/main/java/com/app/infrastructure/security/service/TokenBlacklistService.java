package com.app.infrastructure.security.service;

import java.time.Instant;

/**
 * Tracks revoked JWT IDs ({@code jti}) so that logged-out or rotated tokens are
 * rejected before their natural expiry.
 */
public interface TokenBlacklistService {

    /**
     * Blacklists a token id until its natural expiry.
     *
     * @param jti       the JWT id to revoke
     * @param expiresAt the token's expiry; the blacklist entry is evicted at this time
     */
    void blacklist(String jti, Instant expiresAt);

    /** @return {@code true} if the given {@code jti} has been revoked. */
    boolean isBlacklisted(String jti);
}
