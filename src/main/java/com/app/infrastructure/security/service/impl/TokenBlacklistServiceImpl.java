package com.app.infrastructure.security.service.impl;

import com.app.common.constants.SecurityConstants;
import com.app.infrastructure.security.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis-backed token blacklist. Each revoked {@code jti} is stored under
 * {@link SecurityConstants#TOKEN_BLACKLIST_PREFIX} with a TTL equal to the token's
 * remaining lifetime, so entries self-evict once the token would have expired anyway.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            // Already expired — nothing to revoke.
            return;
        }
        redisTemplate.opsForValue().set(key(jti), "1", ttl);
        log.debug("Blacklisted jti={} for {}s", jti, ttl.toSeconds());
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }

    private String key(String jti) {
        return SecurityConstants.TOKEN_BLACKLIST_PREFIX + jti;
    }
}
