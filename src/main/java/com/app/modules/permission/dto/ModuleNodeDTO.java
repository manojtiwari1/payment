package com.app.modules.permission.dto;

import com.app.modules.permission.dto.PermissionNodeDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ModuleNodeDTO {

    private Long moduleId;
    private String moduleName;
    private Integer selected;
    private Integer total;
    private List<PermissionNodeDTO> permissions;
    private List<ModuleNodeDTO> subModules;

}
