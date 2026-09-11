package com.basilcode.emsbackend.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Every field is optional — {@code EmployeeServices.updateEmployee} only applies the ones
 * present on the request. firstName/lastName/email are set at creation only (email is the
 * User's login identity) and aren't part of this endpoint.
 */
@Data
public class EmployeeUpdateRequest {

    private LocalDate hireDate;

    @Size(max = 150, message = "Job title cannot exceed 150 characters")
    private String jobTitle;

    @DecimalMin(value = "0.0", inclusive = true, message = "Salary cannot be negative")
    private BigDecimal salary;

    private UUID departmentId;

    private Set<UUID> roleIds;
}
