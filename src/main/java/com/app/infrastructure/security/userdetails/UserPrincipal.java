package com.app.infrastructure.security.userdetails;

import com.app.common.constants.SecurityConstants;
import com.app.common.enums.Status;
import com.app.modules.permission.entity.Permission;
import com.app.modules.permission.entity.RolePermission;
import com.app.modules.role.entity.UserRole;
import com.app.modules.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Application principal placed in the {@link org.springframework.security.core.context.SecurityContext}.
 *
 * <p>This is the single principal type across the whole module: it is produced by
 * {@link AppUserDetailsService} during login and rebuilt by the JWT filter on every
 * authenticated request. Authorities are role slugs (prefixed with {@code ROLE_}) plus
 * permission slugs, allowing both {@code hasRole(...)} and {@code hasAuthority(...)} checks.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String username;
    private final String password;
    private final String merchantCode;
    private final boolean active;
    private final Set<GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String username, String password,
                         String merchantCode, boolean active, Set<GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.merchantCode = merchantCode;
        this.active = active;
        this.authorities = authorities;
    }

    /**
     * Builds a principal from a fully-loaded {@link User} (roles + permissions fetched).
     * Inactive roles/permissions are ignored.
     */
    public static UserPrincipal from(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (UserRole userRole : user.getUserRoles()) {
            if (userRole.getRole() == null || !Status.ACTIVE.equals(userRole.getRole().getStatus())) {
                continue;
            }
            String slug = userRole.getRole().getSlug();
            if (slug != null && !slug.isBlank()) {
                String normalized = slug.toUpperCase();
                if (!normalized.startsWith(SecurityConstants.ROLE_PREFIX)) {
                    normalized = SecurityConstants.ROLE_PREFIX + normalized;
                }
                authorities.add(new SimpleGrantedAuthority(normalized));
            }

            for (RolePermission rp : userRole.getRole().getRolePermissions()) {
                Permission permission = rp.getPermission();
                if (permission != null
                        && Status.ACTIVE.equals(permission.getStatus())
                        && permission.getSlug() != null) {
                    authorities.add(new SimpleGrantedAuthority(permission.getSlug().toUpperCase()));
                }
            }
        }

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPassword(),
                user.getMerchantCode(),
                Status.ACTIVE.equals(user.getStatus()),
                authorities
        );
    }

    /**
     * Rebuilds a principal directly from JWT claims (no DB hit) for the request filter path.
     */
    public static UserPrincipal of(Long id, String email, String username, String merchantCode,
                                   Set<String> authorityStrings) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (authorityStrings != null) {
            for (String a : authorityStrings) {
                if (a != null && !a.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(a));
                }
            }
        }
        return new UserPrincipal(id, email, username, null, merchantCode, true, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** The username used by Spring Security is the user's e-mail. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
