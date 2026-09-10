package com.basilcode.emsbackend.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.basilcode.emsbackend.role.dto.RoleResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeResponse {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePictureUrl;
    private String jobTitle;
    private BigDecimal salary;
    private LocalDate hireDate;
    private Boolean active;
    private UUID departmentId;
    private String departmentName;
    private Set<RoleResponse> roles;
}
