package com.app.modules.role.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

//@Data
//public class RoleRequest {
//
//    @NotBlank(message = "Role name may not be empty.")
//    @Size(max = 100, message = "Role name cannot exceed 100 characters")
//    private String roleName;
//
//    @Size(max = 255, message = "Description cannot exceed 255 characters")
//    private String description;
//
//    private Status status = Status.ACTIVE;
//
//    @NotEmpty(message = "At least one permission must be selected")
//    private List<Long> permissionIds;
//
//}

@Data
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 100)
    private String roleName;

    @Size(max = 255)
    private String description;

    private Set<Long> permissionIds;

    private Set<Long> userIds;

}
