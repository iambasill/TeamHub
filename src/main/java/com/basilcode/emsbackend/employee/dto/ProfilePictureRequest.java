package com.basilcode.emsbackend.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfilePictureRequest {
    @NotBlank
    private String profilePictureUrl;
}
