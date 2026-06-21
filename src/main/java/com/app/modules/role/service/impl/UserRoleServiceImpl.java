package com.app.modules.role.service.impl;

import com.app.common.enums.ResponseCode;
import com.app.common.exception.ApplicationException;
import com.app.common.exception.ResourceNotFoundException;
import com.app.modules.role.entity.Role;
import com.app.modules.role.entity.UserRole;
import com.app.modules.role.repository.RoleRepository;
import com.app.modules.role.repository.UserRoleRepository;
import com.app.modules.role.service.UserRoleService;
import com.app.modules.user.entity.User;
import com.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    @Transactional
    public void assignSingleRole(Long userId, Long roleId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));

        userRoleRepository.deleteByUserId(userId);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        userRoleRepository.save(userRole);
    }



    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate role exists
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (role.getMaster()) {
            throw new ApplicationException(
                    ResponseCode.ACCESS_DENIED,
                    "Master role cannot be removed"
            );
        }

        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new ApplicationException(
                    ResponseCode.NOT_FOUND,
                    "User does not have this role assigned"
            );
        }

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    @Transactional
    public void removeRoleFromAllUsers(Long roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (role.getMaster()) {
            throw new ApplicationException(
                    ResponseCode.ACCESS_DENIED,
                    "Master role cannot be removed from users"
            );
        }

        if (!userRoleRepository.existsByRoleId(roleId)) {
            return; // nothing to remove
        }

        userRoleRepository.deleteAllByRoleId(roleId);
    }

    public void assignSingleRoleToUser(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }

}

