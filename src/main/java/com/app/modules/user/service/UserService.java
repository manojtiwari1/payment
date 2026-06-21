package com.app.modules.user.service;

import com.app.modules.user.request.ChangePasswordRequestDTO;
import com.app.modules.user.request.UpdateProfileRequestDTO;
import com.app.modules.user.request.UserRequestDTO;
import com.app.modules.user.response.JwtResponse;
import com.app.modules.user.response.UserResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract for user management operations.
 *
 * <p>Authentication concerns (login / logout / token refresh) are handled by
 * {@link com.app.infrastructure.security.service.AuthService}.
 */
public interface UserService {

    /** Creates a user and returns a fresh JWT token pair so the new account is logged in immediately. */
    JwtResponse createUser(@Valid UserRequestDTO request);

    UserResponseDTO getUserById(Long userId);

    UserResponseDTO changeStatus(Long userId);

    UserResponseDTO updateProfile(UpdateProfileRequestDTO request, MultipartFile file);

    /** Triggers a password-reset flow for the given address. */
    void triggerPasswordReset(String email);

    /** Verifies the current password, then updates it to the new value. */
    String changePassword(@Valid ChangePasswordRequestDTO request);

    UserResponseDTO getCurrentUserProfile();

    Page<UserResponseDTO> getAllUsers(Pageable pageable);

    void deleteUser(Long userId);
}
