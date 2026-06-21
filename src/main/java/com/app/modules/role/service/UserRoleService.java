package com.app.modules.role.service;

import com.app.modules.role.entity.Role;
import com.app.modules.user.entity.User;

public interface UserRoleService {

    void assignSingleRole(Long userId, Long roleId);

//    void assignSingleRoleToMultipleUsers(Long roleId, BulkRoleAssignRequestDTO request);

    void assignSingleRoleToUser(User user, Role role);

    void removeRoleFromUser(Long userId, Long roleId);

    void removeRoleFromAllUsers(Long roleId);

}