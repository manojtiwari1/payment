package com.app.infrastructure.security.service.impl;

import com.app.common.enums.ResponseCode;
import com.app.common.enums.Status;
import com.app.common.exception.ApplicationException;
import com.app.infrastructure.messaging.EventPublisher;
import com.app.infrastructure.security.events.UserLoggedInEvent;
import com.app.infrastructure.security.events.UserLoggedOutEvent;
import com.app.infrastructure.security.jwt.JwtService;
import com.app.infrastructure.security.jwt.TokenType;
import com.app.infrastructure.security.service.AuthService;
import com.app.infrastructure.security.service.TokenBlacklistService;
import com.app.infrastructure.security.userdetails.UserPrincipal;
import com.app.modules.user.entity.User;
import com.app.modules.user.repository.UserRepository;
import com.app.modules.user.request.LogInRequestDTO;
import com.app.modules.user.request.RefreshTokenRequestDTO;
import com.app.modules.user.response.JwtResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Self-contained JWT authentication service.
 *
 * <p>Issues HMAC-signed access + refresh tokens, verifies passwords with the configured
 * {@link PasswordEncoder}, and supports revocation/rotation through a Redis-backed
 * {@link TokenBlacklistService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final EventPublisher eventPublisher;

    @Value("${security.jwt.expiration}")
    private long accessTokenExpirationMs;

    @Value("${security.jwt.refresh-token.expiration}")
    private long refreshTokenExpirationMs;

    // ── Login ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public JwtResponse doLogin(LogInRequestDTO request) {
        // Delegates credential + account-status checks to the DaoAuthenticationProvider
        // (via AppUserDetailsService); the resolved principal carries authorities.
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUserName(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Invalid username or password.");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApplicationException(ResponseCode.NOT_FOUND, "User not found."));
        user.setLastLoggedInAt(Instant.now());
        userRepository.save(user);

        eventPublisher.publish(new UserLoggedInEvent(
                String.valueOf(user.getId()), user.getEmail(), Instant.now()));

        // The principal from the AuthenticationManager already carries authorities.
        return buildTokenPair(principal, user);
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponse issueTokensFor(User user) {
        User fullUser = userRepository.findUserWithRolesAndPermissions(user.getId()).orElse(user);
        return buildTokenPair(UserPrincipal.from(fullUser), user);
    }

    // ── Token refresh (with rotation) ───────────────────────────────────────────

    @Override
    @Transactional
    public JwtResponse refreshAccessToken(RefreshTokenRequestDTO request) {
        String refreshToken = request.getRefreshToken();

        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception e) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED, "Invalid or expired refresh token.");
        }

        if (TokenType.REFRESH != jwtService.getType(claims)) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED, "Provided token is not a refresh token.");
        }

        String jti = jwtService.getJti(claims);
        if (tokenBlacklistService.isBlacklisted(jti)) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED, "Refresh token has been revoked.");
        }

        Long userId = jwtService.getUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ResponseCode.ACCESS_DENIED, "User no longer exists."));

        if (!Status.ACTIVE.equals(user.getStatus())) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED, "User account is not active.");
        }

        // Rotate: revoke the presented refresh token so it cannot be reused.
        tokenBlacklistService.blacklist(jti, jwtService.getExpiration(claims));

        User fullUser = userRepository.findUserWithRolesAndPermissions(user.getId()).orElse(user);
        return buildTokenPair(UserPrincipal.from(fullUser), user);
    }

    // ── Logout ──────────────────────────────────────────────────────────────────

    @Override
    public void doLogout(String accessToken, String refreshToken) {
        String subject = blacklistIfPresent(accessToken);
        blacklistIfPresent(refreshToken);

        if (subject != null) {
            eventPublisher.publish(new UserLoggedOutEvent(subject, Instant.now()));
        }
    }

    // ── Current user ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED, "Unauthorized.");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApplicationException(ResponseCode.NOT_FOUND, "User not found."));
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    /** Blacklists a token's jti until its expiry; returns the token subject (or null). */
    private String blacklistIfPresent(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = jwtService.parse(token);
            tokenBlacklistService.blacklist(jwtService.getJti(claims), jwtService.getExpiration(claims));
            return claims.getSubject();
        } catch (Exception e) {
            log.debug("Could not blacklist token: {}", e.getMessage());
            return null;
        }
    }

    /** Generates an access + refresh token pair for the principal and wraps it with the user's profile. */
    private JwtResponse buildTokenPair(UserPrincipal principal, User user) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        return buildJwtResponse(user, accessToken, refreshToken);
    }

    private JwtResponse buildJwtResponse(User user, String accessToken, String refreshToken) {
        return new JwtResponse(
                user.getFirstName(), user.getLastName(), user.getPhoneNumber(),
                accessToken, refreshToken,
                accessTokenExpirationMs, refreshTokenExpirationMs,
                user.getId(), user.getEmail()
        );
    }
}
