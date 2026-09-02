package com.basilcode.emsbackend.role.mapper;

import com.basilcode.emsbackend.role.dto.RoleResponse;
import com.basilcode.emsbackend.role.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toResponse(Role role);
}
