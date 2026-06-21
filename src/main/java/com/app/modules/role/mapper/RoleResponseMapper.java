package com.app.modules.role.mapper;

import com.app.common.mapper.EntityMapper;
import com.app.modules.role.entity.Role;
import com.app.modules.role.response.RoleMiniResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleResponseMapper extends EntityMapper<RoleMiniResponse, Role> {

}