package com.app.modules.user.request;

import lombok.Data;

/**
 * Logout request payload.
 *
 * {@code refreshToken} is optional:
 * - Owning apps supply it so Keycloak can revoke the full session.
 * - SSO guest apps (no refresh token) may omit it; the backend still
 *   blacklists the access token and pushes the SSE logout event to all
 *   other open sessions.
 */
@Data
public class LogoutRequestDTO {
    private String refreshToken;
}
