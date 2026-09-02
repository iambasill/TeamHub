package com.basilcode.emsbackend.employee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
public class EmployeeRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;

    @Size(max = 150, message = "Job title cannot exceed 150 characters")
    private String jobTitle;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Salary cannot be negative")
    private BigDecimal salary;

    @NotNull(message = "Department is required")
    private UUID departmentId;

    private Set<UUID> roleIds;
}
