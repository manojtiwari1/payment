package com.app.infrastructure.security.config;

import com.app.infrastructure.security.userdetails.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enables JPA auditing and resolves the current auditor (for {@code @CreatedBy} /
 * {@code @LastModifiedBy} on {@code AbstractAuditableModel}) from the security context.
 *
 * <p>Falls back to {@code "SYSTEM"} when there is no authenticated principal (e.g. the
 * bootstrap user-creation flow or scheduled jobs).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    private static final String SYSTEM = "SYSTEM";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of(SYSTEM);
            }
            if (auth.getPrincipal() instanceof UserPrincipal principal) {
                return Optional.of(principal.getEmail());
            }
            return Optional.of(SYSTEM);
        };
    }
}
