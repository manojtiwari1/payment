package com.app.infrastructure.security.jwt;

import com.app.common.constants.SecurityConstants;
import com.app.infrastructure.security.userdetails.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Self-contained JWT mint/verify built on jjwt 0.13.
 *
 * <p>Tokens are signed with an HMAC key derived from the Base64-encoded
 * {@code security.jwt.secret-key} property. Both access and refresh tokens carry a
 * {@code type} claim ({@link TokenType}) plus identity/authorization claims so the
 * request filter can rebuild a {@link UserPrincipal} without a database round-trip.
 */
@Slf4j
@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${security.jwt.secret-key}") String secret,
            @Value("${security.jwt.expiration}") long accessTokenExpirationMs,
            @Value("${security.jwt.refresh-token.expiration}") long refreshTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    // ── Generation ────────────────────────────────────────────────────────────

    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, TokenType.ACCESS, accessTokenExpirationMs);
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, TokenType.REFRESH, refreshTokenExpirationMs);
    }

    private String buildToken(UserPrincipal principal, TokenType type, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(principal.getId()))
                .claim(SecurityConstants.EMAIL_KEY, principal.getEmail())
                .claim(SecurityConstants.USERNAME_KEY, principal.getUsername())
                .claim(SecurityConstants.MERCHANT_CODE_KEY, principal.getMerchantCode())
                .claim("authorities", authorities)
                .claim("type", type.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // ── Parsing / validation ────────────────────────────────────────────────────

    /** Parses and verifies the signature, returning the claims. Throws on invalid/expired tokens. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Verifies signature + expiry without throwing. */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public TokenType getType(Claims claims) {
        String type = claims.get("type", String.class);
        return type == null ? null : TokenType.valueOf(type);
    }

    public Long getUserId(Claims claims) {
        String sub = claims.getSubject();
        return sub == null ? null : Long.valueOf(sub);
    }

    public String getEmail(Claims claims) {
        return claims.get(SecurityConstants.EMAIL_KEY, String.class);
    }

    public String getUsername(Claims claims) {
        return claims.get(SecurityConstants.USERNAME_KEY, String.class);
    }

    public String getMerchantCode(Claims claims) {
        return claims.get(SecurityConstants.MERCHANT_CODE_KEY, String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getAuthorities(Claims claims) {
        Object raw = claims.get("authorities");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Set.of();
    }

    /** Expiry as {@link java.time.Instant}, used for blacklist TTL computation. */
    public java.time.Instant getExpiration(Claims claims) {
        Date exp = claims.getExpiration();
        return exp == null ? null : exp.toInstant();
    }
}
