package com.app.modules.role.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class RoleResponse {

    private Long id;

    private String roleName;

    private String slug;

    private String description;

    private String status;

    private Boolean master;

    private Set<String> permissions;

}
