package com.app.modules.permission.controller;

import com.app.common.response.BaseResponse;
import com.app.common.response.Response;
import com.app.modules.permission.dto.PermissionResponseDTO;
import com.app.modules.permission.request.PermissionRequestDTO;
import com.app.modules.permission.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionController extends BaseResponse {

    private final PermissionService permissionService;

    // Create a new permission
    @PostMapping
    public ResponseEntity<Response> createPermission(@RequestBody @Valid PermissionRequestDTO request) {
        PermissionResponseDTO response = permissionService.createPermission(request);
        return data(response);
    }

    // Get a permission by ID
    @GetMapping("/{id}")
    public ResponseEntity<Response> getPermission(@PathVariable("id") Long id) {
        PermissionResponseDTO response = permissionService.getPermissionById(id);
        return data(response);
    }

    // Get all permissions
    @GetMapping
    public ResponseEntity<Response> getAllPermissions() {
        List<PermissionResponseDTO> response = permissionService.getAllPermissions();
        return data(response);
    }

}
