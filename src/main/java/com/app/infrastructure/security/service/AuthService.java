package com.app.infrastructure.security.service;

import com.app.modules.user.entity.User;
import com.app.modules.user.request.LogInRequestDTO;
import com.app.modules.user.request.RefreshTokenRequestDTO;
import com.app.modules.user.response.JwtResponse;

/**
 * Authentication-lifecycle operations for the self-contained JWT model.
 *
 * <p>Callers must never cast this interface to a concrete type.
 */
public interface AuthService {

    /**
     * Authenticates by username/password and returns a fresh access + refresh token pair
     * together with the user's profile data.
     */
    JwtResponse doLogin(LogInRequestDTO requestDTO);

    /**
     * Rotates the refresh token: validates the supplied refresh token, blacklists it, and
     * returns a brand-new access + refresh pair.
     */
    JwtResponse refreshAccessToken(RefreshTokenRequestDTO requestDTO);

    /**
     * Blacklists the access token's {@code jti} (and the refresh token's, if supplied) so
     * they are rejected before their natural expiry.
     *
     * @param accessToken  raw Bearer token from the {@code Authorization} header
     * @param refreshToken optional refresh token to revoke as well
     */
    void doLogout(String accessToken, String refreshToken);

    /**
     * @return the authenticated {@link User} resolved from the current security context.
     */
    User getCurrentUser();

    /**
     * Issues a fresh access + refresh token pair for the given (already persisted) user,
     * without a password check. Used to auto-authenticate a user right after account creation.
     */
    JwtResponse issueTokensFor(User user);
}
