package com.basilcode.emsbackend.employee.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class EmployeeQuerySearch {

    private String userId;
    private String firstName;
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;
}
