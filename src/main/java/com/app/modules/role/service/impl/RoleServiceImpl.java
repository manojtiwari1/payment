package com.app.modules.role.service.impl;

import com.app.common.enums.ResponseCode;
import com.app.common.enums.Status;
import com.app.common.exception.ApplicationException;
import com.app.common.exception.ResourceNotFoundException;
import com.app.infrastructure.security.service.AuthService;
import com.app.modules.permission.dto.ModuleNodeDTO;
import com.app.modules.permission.dto.PermissionNodeDTO;
import com.app.modules.permission.entity.Modules;
import com.app.modules.permission.entity.Permission;
import com.app.modules.permission.repository.ModuleRepository;
import com.app.modules.permission.repository.PermissionRepository;
import com.app.modules.role.entity.Role;
import com.app.modules.role.entity.UserRole;
import com.app.modules.role.mapper.RoleResponseMapper;
import com.app.modules.role.repository.RoleRepository;
import com.app.modules.role.repository.UserRoleRepository;
import com.app.modules.role.request.RoleRequest;
import com.app.modules.role.response.RoleMemberResponseDTO;
import com.app.modules.role.response.RoleMiniResponse;
import com.app.modules.role.response.RoleResponse;
import com.app.modules.role.service.RoleService;
import com.app.modules.user.entity.User;
import com.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final AuthService authService;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final ModuleRepository moduleRepository;

    private final RoleResponseMapper roleResponseMapper;

    private final UserRoleRepository userRoleRepository;

    private final PermissionRepository  permissionRepository;

    @Override
    public List<RoleMiniResponse> getAllActiveRoles() {
        int currentUserLevel = getCurrentUserLevel();
        List<Role> roles =
                roleRepository.findAllByStatusAndLevelGreaterThanEqual(
                        Status.ACTIVE,
                        currentUserLevel
                );
        return roleResponseMapper.toDto(roles);
    }

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {

        String roleName = normalize(request.getRoleName());
        String description = normalize(request.getDescription());

        if (roleRepository.existsByRoleNameIgnoreCase(roleName)) {
            throw new ApplicationException(ResponseCode.DUPLICATE,"Role name already exists");
        }

        int currentUserLevel = getCurrentUserLevel();

        int maxLevel = roleRepository.findMaxLevel();

        int newRoleLevel = maxLevel + 1;

        // Safety check: ensure user cannot create higher or equal level role
        if (newRoleLevel <= currentUserLevel) {
            throw new ApplicationException(ResponseCode.ACCESS_DENIED, "You cannot create a role higher or equal to your level");
        }

        Role role = new Role();
        role.setRoleName(roleName);
        role.setDescription(description);
        role.setSlug(generateSlug(roleName));
        role.setStatus(Status.ACTIVE);
        role.setMaster(false);
        role.setLevel(newRoleLevel);

        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            Set<Permission> permissions =
                    new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            role.replacePermissions(permissions);
        }

        roleRepository.save(role);

        // NEW: Assign Users to Role
        if (!CollectionUtils.isEmpty(request.getUserIds())) {

            List<User> users = userRepository.findAllActiveByIdIn(request.getUserIds());

            if (users.size() != request.getUserIds().size()) {
                throw new ApplicationException(ResponseCode.BAD_REQUEST, "Some users not found or inactive");
            }

            List<UserRole> userRoles = new ArrayList<>();

            for (User user : users) {
                // Hierarchy safety: cannot assign role to higher-level user
                int targetUserLevel = user.getUserRoles()
                        .stream()
                        .map(userRole -> userRole.getRole().getLevel())
                        .min(Integer::compareTo)
                        .orElse(Integer.MAX_VALUE);

                if (targetUserLevel < currentUserLevel) {
                    throw new ApplicationException(ResponseCode.ACCESS_DENIED, "You cannot modify higher-level users");
                }

                userRoleRepository.deleteByUserId(user.getId());

                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(role);
                userRoles.add(userRole);
            }
            userRoleRepository.saveAll(userRoles);
        }

        return mapToResponse(role);
    }

    @Override
    public RoleResponse update(Long roleId, RoleRequest request) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (role.getMaster()) {
            throw new RuntimeException("Master role cannot be modified");
        }

        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());

        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            Set<Permission> permissions =
                    new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            role.updatePermissions(permissions);
        }

        return mapToResponse(role);
    }

    @Override
    public void delete(Long roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (role.getMaster()) {
            throw new RuntimeException("Master role cannot be deleted");
        }

        boolean assigned = userRoleRepository.existsByRoleId(roleId);
        if (assigned) {
            throw new RuntimeException("Role is assigned to users and cannot be deleted");
        }

        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleMemberResponseDTO> getMembersByRoleId(
            Long roleId,
            Pageable pageable
    ) {

        // Validate role exists
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role not found"));

        Page<User> users = userRepository.findUsersByRoleId(roleId, pageable);

        return users.map(user ->
                RoleMemberResponseDTO.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Long roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        return mapToResponse(role);
    }

    // Helper Methods
    private String generateSlug(String roleName) {

        if (roleName == null) {
            return null;
        }
        return "ROLE_" +
                roleName
                        .trim()
                        .toUpperCase()
                        .replaceAll("\\s+", "_");
    }

    private RoleResponse mapToResponse(Role role) {

        Set<String> permissionSlugs = role.getRolePermissions()
                .stream()
                .map(rp -> rp.getPermission().getSlug())
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .slug(role.getSlug())
                .description(role.getDescription())
                .status(role.getStatus().name())
                .master(role.getMaster())
                .permissions(permissionSlugs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleNodeDTO> getPermissionTree(Long roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Get selected permission IDs
        Set<Long> selectedPermissionIds = role.getRolePermissions()
                .stream()
                .map(rp -> rp.getPermission().getId())
                .collect(Collectors.toSet());

        List<Modules> allModules =
                moduleRepository.findByStatusOrderByDisplayOrderAsc(Status.ACTIVE);

        List<Permission> allPermissions =
                permissionRepository.findByStatus(Status.ACTIVE);

        // Group permissions by module
        Map<Long, List<Permission>> permissionByModule =
                allPermissions.stream()
                        .collect(Collectors.groupingBy(p -> p.getModule().getId()));

        // Build tree
        Map<Long, ModuleNodeDTO> moduleNodeMap = new LinkedHashMap<>();

        for (Modules module : allModules) {

            if (module.getParent() == null) {

                ModuleNodeDTO parentNode =
                        buildModuleNode(module, permissionByModule, selectedPermissionIds);

                moduleNodeMap.put(module.getId(), parentNode);
            }
        }

        // Attach children
        for (Modules module : allModules) {

            if (module.getParent() != null) {

                ModuleNodeDTO childNode =
                        buildModuleNode(module, permissionByModule, selectedPermissionIds);

                ModuleNodeDTO parent =
                        moduleNodeMap.get(module.getParent().getId());

                if (parent != null) {

                    if (parent.getSubModules() == null) {
                        parent.setSubModules(new ArrayList<>());
                    }

                    parent.getSubModules().add(childNode);

                    parent.setSelected(
                            parent.getSelected() + childNode.getSelected());

                    parent.setTotal(
                            parent.getTotal() + childNode.getTotal());
                }
            }
        }

        return new ArrayList<>(moduleNodeMap.values());
    }

    private int getCurrentUserLevel() {

        User currentUser = authService.getCurrentUser();

        return currentUser.getUserRoles()
                .stream()
                .map(userRole -> userRole.getRole().getLevel())
                .min(Integer::compareTo)
                .orElseThrow(() ->
                        new ApplicationException(ResponseCode.ACCESS_DENIED,
                                "User has no assigned role"));
    }

    private ModuleNodeDTO buildModuleNode(
            Modules module,
            Map<Long, List<Permission>> permissionByModule,
            Set<Long> selectedPermissionIds) {

        List<Permission> permissions =
                permissionByModule.getOrDefault(module.getId(), Collections.emptyList());

        List<PermissionNodeDTO> permissionNodes = new ArrayList<>();

        int selectedCount = 0;

        for (Permission permission : permissions) {

            boolean selected = selectedPermissionIds.contains(permission.getId());

            if (selected) {
                selectedCount++;
            }

            permissionNodes.add(
                    PermissionNodeDTO.builder()
                            .permissionId(permission.getId())
                            .permissionName(permission.getPermissionName())
                            .slug(permission.getSlug())
                            .selected(selected)
                            .build()
            );
        }

        return ModuleNodeDTO.builder()
                .moduleId(module.getId())
                .moduleName(module.getName())
                .selected(selectedCount)
                .total(permissionNodes.size())
                .permissions(permissionNodes)
                .subModules(new ArrayList<>())
                .build();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
