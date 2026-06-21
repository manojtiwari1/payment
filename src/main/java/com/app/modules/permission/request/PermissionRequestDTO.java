package com.app.modules.permission.request;

import com.app.common.enums.Status;
import lombok.Data;

@Data
public class PermissionRequestDTO {

    private String permissionName;

    private String description;

    private Status status = Status.ACTIVE;
}
