package com.basilcode.emsbackend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {
    private UUID userId;
    private String emailId;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private Boolean mustChangePassword;
    private String userType;
}
