package com.app.modules.permission.service.impl;
import com.app.common.exception.ResourceNotFoundException;
import com.app.modules.permission.dto.PermissionResponseDTO;
import com.app.modules.permission.entity.Permission;
import com.app.modules.permission.mapper.PermissionRequestMapper;
import com.app.modules.permission.mapper.PermissionResponseDTOMapper;
import com.app.modules.permission.repository.PermissionRepository;
import com.app.modules.permission.request.PermissionRequestDTO;
import com.app.modules.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    private final PermissionRequestMapper permissionRequestMapper;

    private final PermissionResponseDTOMapper permissionResponseDTOMapper;


    // Create a new permission
    @Override
    @Transactional
    public PermissionResponseDTO createPermission(PermissionRequestDTO request) {
        Permission permission = permissionRequestMapper.toEntity(request);
        permissionRepository.save(permission);
        return permissionResponseDTOMapper.toDto(permission);
    }

    // Get permission by ID
    @Override
    public PermissionResponseDTO getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id " + id));
        return permissionResponseDTOMapper.toDto(permission);
    }

    // Get all permissions
    @Override
    public List<PermissionResponseDTO> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissionResponseDTOMapper.toDto(permissions);
    }



}

