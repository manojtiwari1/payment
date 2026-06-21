package com.app.infrastructure.security.userdetails;

import com.app.common.enums.Status;
import com.app.modules.user.entity.User;
import com.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a {@link UserPrincipal} by e-mail (the Spring Security username) with roles and
 * permissions eagerly fetched, enforcing that the account is {@link Status#ACTIVE}.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password."));

        if (!Status.ACTIVE.equals(user.getStatus())) {
            throw new UsernameNotFoundException("User account is not active.");
        }

        // Re-load with roles/permissions so UserPrincipal.from() can resolve authorities.
        User fullUser = userRepository.findUserWithRolesAndPermissions(user.getId())
                .orElse(user);

        return UserPrincipal.from(fullUser);
    }
}
