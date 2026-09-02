package com.basilcode.emsbackend.department.mapper;

import com.basilcode.emsbackend.department.dto.DepartmentResponse;
import com.basilcode.emsbackend.department.entity.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    DepartmentResponse toResponse(Department department);
}
