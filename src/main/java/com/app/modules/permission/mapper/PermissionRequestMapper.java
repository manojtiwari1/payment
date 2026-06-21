package com.app.modules.permission.mapper;

import com.app.common.mapper.EntityMapper;
import com.app.modules.permission.entity.Permission;
import com.app.modules.permission.request.PermissionRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionRequestMapper extends EntityMapper<PermissionRequestDTO, Permission> {
}
