package com.app.modules.permission.dto;

import com.app.common.enums.Status;
import lombok.Data;

@Data
public class PermissionResponseDTO {

    private Long id;
    private Status status;
    private String description;
    private String permissionName;

}