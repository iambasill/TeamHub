package com.basilcode.emsbackend.employee.mapper;

import com.basilcode.emsbackend.employee.dto.EmployeeRequest;
import com.basilcode.emsbackend.employee.dto.EmployeeResponse;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.role.mapper.RoleMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface EmployeeMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "user.emailId", target = "email")
    @Mapping(source = "user.profilePictureUrl", target = "profilePictureUrl")
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    // Never mapped from the entity: the Employee/User rows only hold a bcrypt
    // hash. EmployeeServices sets this explicitly on the create response when
    // app.employee.return-temporary-password is enabled.
    @Mapping(target = "temporaryPassword", ignore = true)
    EmployeeResponse toResponse(Employee employee);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Employee toEntity(EmployeeRequest request);
}