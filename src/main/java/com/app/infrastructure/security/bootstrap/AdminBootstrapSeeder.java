package com.app.infrastructure.security.bootstrap;

import com.app.common.enums.Status;
import com.app.modules.role.entity.Role;
import com.app.modules.role.entity.UserRole;
import com.app.modules.role.repository.RoleRepository;
import com.app.modules.role.repository.UserRoleRepository;
import com.app.modules.user.entity.User;
import com.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a default admin user (with an ADMIN role) the first time the application starts
 * against an <em>empty</em> users table.
 *
 * <p>Without this, the system is un-bootstrappable: the only endpoint that sets a password
 * ({@code POST /api/auth/users}) itself requires a valid access token, and obtaining a token
 * requires a user who already has a password. The seeder breaks that cycle.
 *
 * <p>It is strictly guarded — it does nothing unless {@code userRepository.count() == 0} — so
 * it never touches an existing dataset. Disable it with {@code app.bootstrap.admin.enabled=false}.
 *
 * <p><strong>Existing databases:</strong> rows created before this change have a NULL password
 * (added by {@code ddl-auto=update}) and therefore cannot password-login. Backfill them with a
 * bcrypt hash (e.g. an admin-driven password reset) — the seeder intentionally will not modify them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.email:admin@payment.local}")
    private String email;

    @Value("${app.bootstrap.admin.password:Admin@12345}")
    private String password;

    @Value("${app.bootstrap.admin.first-name:System}")
    private String firstName;

    @Value("${app.bootstrap.admin.last-name:Administrator}")
    private String lastName;

    @Value("${app.bootstrap.admin.role-slug:ADMIN}")
    private String roleSlug;

    @Value("${app.bootstrap.merchant.email:merchant@payment.local}")
    private String merchantEmail;

    @Value("${app.bootstrap.merchant.password:Merchant@12345}")
    private String merchantPassword;

    @Value("${app.bootstrap.merchant.code:M123}")
    private String merchantCode;

    @Value("${app.bootstrap.merchant.role-slug:MERCHANT}")
    private String merchantRoleSlug;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (userRepository.count() > 0) {
            log.debug("Bootstrap skipped: users already exist.");
            return;
        }

        log.info("Empty users table detected — seeding default admin and merchant accounts.");

        Role adminRole = findOrCreateRole(roleSlug, "Administrator",
                "Bootstrapped administrator role with full access.", true, 0);
        seedUser(email, password, firstName, lastName, null, adminRole);

        Role merchantRole = findOrCreateRole(merchantRoleSlug, "Merchant",
                "Bootstrapped merchant role.", false, 1);
        seedUser(merchantEmail, merchantPassword, "Sample", "Merchant", merchantCode, merchantRole);

        log.info("Seeded admin '{}' and merchant '{}' (merchantCode={}). Change these passwords immediately.",
                email, merchantEmail, merchantCode);
    }

    private void seedUser(String email, String rawPassword, String firstName, String lastName,
                          String merchantCode, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email.toLowerCase());
        user.setUsername(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setMerchantCode(merchantCode);
        user.setStatus(Status.ACTIVE);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }

    private Role findOrCreateRole(String slug, String name, String description, boolean master, int level) {
        return roleRepository.findAllByStatusAndLevelGreaterThanEqual(Status.ACTIVE, 0).stream()
                .filter(r -> slug.equalsIgnoreCase(r.getSlug()))
                .findFirst()
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName(name);
                    role.setSlug(slug.toUpperCase());
                    role.setDescription(description);
                    role.setStatus(Status.ACTIVE);
                    role.setMaster(master);
                    role.setLevel(level);
                    return roleRepository.save(role);
                });
    }
}
