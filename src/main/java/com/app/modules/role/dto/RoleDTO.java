package com.app.modules.role.dto;

import com.app.common.enums.Status;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class RoleDTO {

    private Long id;

    private String roleName;

    private Status status;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;

    private List<Long> permissionIds;

}
