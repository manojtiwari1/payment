package com.app.modules.permission.mapper;

import com.app.common.mapper.EntityMapper;
import com.app.modules.permission.dto.PermissionResponseDTO;
import com.app.modules.permission.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionResponseDTOMapper extends EntityMapper<PermissionResponseDTO, Permission> {
}
