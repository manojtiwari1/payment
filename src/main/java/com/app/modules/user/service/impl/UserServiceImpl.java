package com.app.modules.user.service.impl;

import com.app.common.constants.AppConstants;
import com.app.common.enums.PictureType;
import com.app.common.enums.ResponseCode;
import com.app.common.enums.Status;
import com.app.common.exception.ApplicationException;
import com.app.common.exception.ResourceNotFoundException;
import com.app.infrastructure.security.service.AuthService;
import com.app.modules.permission.entity.Permission;
import com.app.modules.permission.entity.RolePermission;
import com.app.modules.picture.model.Picture;
import com.app.modules.role.entity.Role;
import com.app.modules.role.entity.UserRole;
import com.app.modules.role.repository.RoleRepository;
import com.app.modules.role.service.UserRoleService;
import com.app.modules.user.entity.User;
import com.app.modules.user.mapper.UserDTOMapper;
import com.app.modules.user.repository.UserRepository;
import com.app.modules.user.request.ChangePasswordRequestDTO;
import com.app.modules.user.request.UpdateProfileRequestDTO;
import com.app.modules.user.request.UserRequestDTO;
import com.app.modules.user.response.JwtResponse;
import com.app.modules.user.response.UserResponseDTO;
import com.app.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Value("${file.upload.max-size:15728640}")
    private long maxFileSize;

    private final AuthService authService;
    private final UserDTOMapper userDTOMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    // User creation

    @Override
    @Transactional
    public JwtResponse createUser(UserRequestDTO request) {
        String firstName       = trimmed(request.getFirstName());
        String lastName        = trimmed(request.getLastName());
        String email           = trimmed(request.getEmail());
        String phone           = trimmed(request.getMobileNo());
        String password        = trimmed(request.getPassword());
        String confirmPassword = trimmed(request.getConfirmPassword());

        validateRequiredNameAndEmail(firstName, lastName, email);

        if (password == null || password.isBlank()) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Password is required.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Passwords do not match.");
        }
        if (request.getRoleId() == null) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "A role must be assigned to the user.");
        }

        assertEmailNotTaken(email, null);
        assertPhoneNotTaken(phone, null);

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
        if (!Status.ACTIVE.equals(role.getStatus())) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Inactive role cannot be assigned.");
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email.toLowerCase());
        user.setUsername(email.toLowerCase());
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setMerchantCode(trimmed(request.getMerchantCode()));
        user.setStatus(Status.ACTIVE);
        user = userRepository.save(user);
        userRoleService.assignSingleRoleToUser(user, role);

        // Auto-authenticate the freshly created account: issue an access + refresh token pair.
        return authService.issueTokensFor(user);
    }

    // Read operations

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId) {
        return userDTOMapper.toDto(requireUser(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUserProfile() {
        User userRef = authService.getCurrentUser();

        User user = userRepository.findUserWithRolesAndPermissions(userRef.getId())
                .orElseThrow(() -> new ApplicationException(ResponseCode.NOT_FOUND, "User not found."));

        if (!Status.ACTIVE.equals(user.getStatus())) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED, "User account is inactive.");
        }

        return buildFullUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userDTOMapper::toDto);
    }

    // Profile update

    @Override
    @Transactional
    public UserResponseDTO updateProfile(UpdateProfileRequestDTO request, MultipartFile file) {
        User currentUser = resolveCurrentUser();

        String firstName = trimmed(request.getFirstName());
        String lastName  = trimmed(request.getLastName());
        String email     = trimmed(request.getEmail());
        String phone     = trimmed(request.getPhoneNumber());

        validateRequiredNameAndEmail(firstName, lastName, email);
        assertEmailNotTaken(email, currentUser.getId());
        assertPhoneNotTaken(phone, currentUser.getId());

        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setEmail(email.toLowerCase());
        currentUser.setUsername(email.toLowerCase());
        currentUser.setPhoneNumber(phone);

        if (file != null && !file.isEmpty()) {
            validateProfileImage(file);
            String key = buildProfilePictureKey(currentUser.getId(), file.getOriginalFilename());
            currentUser.setProfilePictureUrl(key);

            Picture picture = new Picture();
            picture.setType(PictureType.PROFILE_PICTURE);
            picture.setUrl(key);
            currentUser.setPicture(picture);

            log.info("Profile picture key set for user {}: {}", currentUser.getId(), key);
        }

        userRepository.save(currentUser);
        return userDTOMapper.toDto(currentUser);
    }

    // Status management

    @Override
    @Transactional
    public UserResponseDTO changeStatus(Long userId) {
        User user = requireUser(userId);
        user.setStatus(Status.ACTIVE.equals(user.getStatus()) ? Status.INACTIVE : Status.ACTIVE);
        userRepository.save(user);
        log.info("User {} status changed to {}", userId, user.getStatus());
        return userDTOMapper.toDto(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = authService.getCurrentUser();
        User user = requireUser(userId);

        if (Status.DELETED.equals(user.getStatus())) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "User is already deleted.");
        }

        user.setStatus(Status.DELETED);
        user.setDeletedAt(Instant.now());
        user.setDeletedBy(currentUser.getCreatedBy());
        userRepository.save(user);
        log.info("User {} soft-deleted by {}", userId, currentUser.getId());
    }


    @Override
    public void triggerPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Email is required.");
        }
        // Verify the user exists locally before initiating the reset flow.
        userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ApplicationException(ResponseCode.NOT_FOUND, "User not found."));
    }

    /**
     * Verifies the current password against the stored BCrypt hash, then stores the
     * new (encoded) password.
     */
    @Override
    @Transactional
    public String changePassword(ChangePasswordRequestDTO request) {
        User user = authService.getCurrentUser();

        if (user.getPassword() == null
                || request.getCurrentPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED, "Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user {}", user.getId());
        return "Password changed successfully.";
    }


    // private helpers

    private User resolveCurrentUser() {
        return authService.getCurrentUser();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found."));
    }



    private void validateRequiredNameAndEmail(String firstName, String lastName, String email) {
        if (firstName == null || firstName.isBlank()) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "First name is required.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Last name is required.");
        }
        if (email == null || email.isBlank()) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST, "Email address is required.");
        }
    }

    private void assertEmailNotTaken(String email, Long excludeUserId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(excludeUserId)) {
                throw new ApplicationException(ResponseCode.DUPLICATE, "Email address is already in use.");
            }
        });
    }

    private void assertPhoneNotTaken(String phone, Long excludeUserId) {
        if (phone == null || phone.isBlank()) return;
        userRepository.findByPhoneNumber(phone).ifPresent(existing -> {
            if (!existing.getId().equals(excludeUserId)) {
                throw new ApplicationException(ResponseCode.DUPLICATE, "Phone number is already in use.");
            }
        });
    }

    private void validateProfileImage(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new ApplicationException(ResponseCode.FILE_SIZE_EXCEED,
                    "File exceeds the maximum allowed size.");
        }
        if (!AppConstants.ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ApplicationException(ResponseCode.BAD_REQUEST,
                    "Unsupported image format. Allowed: JPEG, PNG, WebP.");
        }
    }

    private String buildProfilePictureKey(Long userId, String originalFilename) {
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : ".jpg";
        return "users/" + userId + "/profile/" + UUID.randomUUID() + ext;
    }



    private UserResponseDTO buildFullUserResponse(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setStatus(user.getStatus());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setUpdatedBy(user.getUpdatedBy());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());

        Set<UserRole> activeUserRoles = user.getUserRoles().stream()
                .filter(ur -> ur.getRole() != null && Status.ACTIVE.equals(ur.getRole().getStatus()))
                .collect(Collectors.toSet());

        Set<String> roleSlugs = activeUserRoles.stream()
                .map(ur -> ur.getRole().getSlug())
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s)
                .collect(Collectors.toSet());

        Set<String> roleNames = activeUserRoles.stream()
                .map(ur -> ur.getRole().getRoleName())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> permissionSlugs = activeUserRoles.stream()
                .flatMap(ur -> ur.getRole().getRolePermissions().stream())
                .filter(Objects::nonNull)
                .map(RolePermission::getPermission)
                .filter(p -> p != null && Status.ACTIVE.equals(p.getStatus()))
                .map(Permission::getSlug)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        dto.setRoleSlugs(roleSlugs);
        dto.setRoles(roleNames);
        dto.setPermissions(permissionSlugs);
        return dto;
    }

    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
