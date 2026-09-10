package com.basilcode.emsbackend.employee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    /**
     * The generated sign-in password, in plain text.
     *
     * Present ONLY on the create response, and only when
     * app.employee.return-temporary-password is enabled. It is the same value
     * that is emailed, returned to the HR/Admin caller who just created the
     * account so provisioning still works when SMTP is unavailable.
     *
     * NON_NULL keeps it out of the JSON entirely everywhere else, so list and
     * detail payloads are unchanged rather than gaining a null field.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String temporaryPassword;
}
