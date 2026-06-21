package com.app.common.constants;

/**
 * @author Manoj Tiwari
 * @since 01-04-2026
 * @implSpec Centralized constants for security-related configurations, such as public endpoints,
 * JWT claim keys, and permission slugs. This promotes consistency across the application and makes
 * it easier to manage security settings in one place.
 */

public class SecurityConstants {

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/verify-token",
            "/api/auth/google",
            "/api/devices/register",
            "/api/auth/users/**",
            "/webhooks/**"
    };

    public static final String[] SWAGGER_END_POINTS ={
            "/swagger-ui/**",
            "/swagger-ui/swagger-ui.css.map",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/webjars/**"

    };

    public static final String ID_KEY = "id";
    public static final String NAME_KEY = "name";
    public static final String USERNAME_KEY = "username";
    public static final String EMAIL_KEY = "email";
    public static final String ROLES_KEY = "roles";
    public static final String PERMISSIONS_KEY = "permissions";
    public static final String JTI_KEY = "jti";
    public static final String MERCHANT_CODE_KEY = "merchantCode";

    // ── App-level access permission slugs ─────────────────────────────────────
    // These are seeded as Permission records in the DB and assigned to roles.
    // Each application guards its endpoints with @PreAuthorize("hasAuthority('...')")
    public static final String ADMIN_APP_ACCESS   = "ADMIN_APP_ACCESS";

    // Redis key prefix used to blacklist revoked JWT IDs on logout
    public static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    public static final String ROLE_PREFIX="ROLE_";
}
