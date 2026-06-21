package com.app.modules.user.mapper;

import com.app.common.mapper.EntityMapper;
import com.app.modules.user.entity.User;
import com.app.modules.user.response.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserDTOMapper extends EntityMapper<UserResponseDTO, User> {
}