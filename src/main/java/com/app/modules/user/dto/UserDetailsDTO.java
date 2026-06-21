package com.app.modules.user.dto;

import com.app.common.enums.Status;
import com.app.modules.role.dto.RoleDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class UserDetailsDTO {


    private final Long id;

    private final String firstName;

    private final String lastName;

    private final String email;

    private final String mobileNo;

    private final Status status;

    private Long roleId;

    private String roleName;

    @JsonIgnore
    private RoleDTO role;
}
