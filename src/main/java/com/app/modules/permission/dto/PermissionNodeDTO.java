package com.app.modules.permission.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionNodeDTO {

    private Long permissionId;
    private String permissionName;
    private String slug;
    private boolean selected;

}
