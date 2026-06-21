package com.app.infrastructure.security.jwt;

import com.app.common.constants.AppConstants;
import com.app.infrastructure.security.service.TokenBlacklistService;
import com.app.infrastructure.security.userdetails.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates each request from a {@code Bearer} access token.
 *
 * <p>Validates the signature/expiry, requires {@code type=ACCESS}, rejects tokens whose
 * {@code jti} has been blacklisted, then rebuilds a {@link UserPrincipal} from the claims
 * and stores it in the {@link SecurityContextHolder}. On any failure the context is left
 * empty and the request proceeds — {@link JwtAuthenticationEntryPoint} produces the 401
 * if the target endpoint requires authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(token);

                if (TokenType.ACCESS != jwtService.getType(claims)) {
                    log.debug("Rejected non-access token on protected request");
                } else if (tokenBlacklistService.isBlacklisted(jwtService.getJti(claims))) {
                    log.debug("Rejected blacklisted token jti={}", jwtService.getJti(claims));
                } else {
                    UserPrincipal principal = UserPrincipal.of(
                            jwtService.getUserId(claims),
                            jwtService.getEmail(claims),
                            jwtService.getUsername(claims),
                            jwtService.getMerchantCode(claims),
                            jwtService.getAuthorities(claims));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null, principal.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // Invalid/expired token — leave context empty; entry point handles 401.
                log.debug("JWT authentication failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AppConstants.AUTH_HEADER);
        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            String token = header.substring(AppConstants.BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
