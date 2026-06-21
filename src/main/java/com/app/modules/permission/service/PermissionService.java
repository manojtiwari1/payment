package com.app.modules.permission.service;

import com.app.modules.permission.dto.PermissionResponseDTO;
import com.app.modules.permission.request.PermissionRequestDTO;

import java.util.List;

public interface PermissionService {

    PermissionResponseDTO createPermission(PermissionRequestDTO request);

    PermissionResponseDTO getPermissionById(Long id);

    List<PermissionResponseDTO> getAllPermissions();
}
