package com.app.modules.role.service;

import com.app.modules.permission.dto.ModuleNodeDTO;
import com.app.modules.role.request.RoleRequest;
import com.app.modules.role.response.RoleMemberResponseDTO;
import com.app.modules.role.response.RoleMiniResponse;
import com.app.modules.role.response.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoleService {

    List<RoleMiniResponse> getAllActiveRoles();

    RoleResponse create(RoleRequest request);

    RoleResponse update(Long roleId, RoleRequest request);

    void delete(Long roleId);

    RoleResponse getById(Long roleId);

    List<ModuleNodeDTO> getPermissionTree(Long roleId);

    Page<RoleMemberResponseDTO> getMembersByRoleId(Long roleId, Pageable pageable);

}
