package com.app.modules.role.response;

import com.app.common.enums.Status;
import lombok.Data;

@Data
public class RoleMiniResponse {

    private Long id;

    private String roleName;

    private Status status;

    private String description;

}