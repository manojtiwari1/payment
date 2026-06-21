package com.app.infrastructure.security.controller;

import com.app.common.constants.AppConstants;
import com.app.common.response.BaseResponse;
import com.app.common.response.Response;
import com.app.infrastructure.security.service.AuthService;
import com.app.modules.user.request.LogInRequestDTO;
import com.app.modules.user.request.LogoutRequestDTO;
import com.app.modules.user.request.RefreshTokenRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints: login, refresh, logout.
 *
 * <p>{@code /api/auth/login} and {@code /api/auth/refresh} are public (see
 * {@code SecurityConstants.PUBLIC_ENDPOINTS}); {@code /api/auth/logout} requires a valid
 * access token.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseResponse {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Response> login(@Valid @RequestBody LogInRequestDTO request) {
        return data(authService.doLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Response> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return data(authService.refreshAccessToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Response> logout(HttpServletRequest httpRequest,
                                           @RequestBody(required = false) LogoutRequestDTO request) {
        String accessToken = resolveBearerToken(httpRequest);
        String refreshToken = request == null ? null : request.getRefreshToken();
        authService.doLogout(accessToken, refreshToken);
        return success(com.app.common.enums.ResponseCode.ENTITY, "Logged out successfully.");
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AppConstants.AUTH_HEADER);
        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            return header.substring(AppConstants.BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
