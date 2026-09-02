package com.basilcode.emsbackend.auth.mapper;

import com.basilcode.emsbackend.auth.dto.AuthResponseDto;
import com.basilcode.emsbackend.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public class AuthMapper {
    public AuthResponseDto toAuthResponseDto(User user) {
        return  new AuthResponseDto(
                user.getId(),
                user.getEmailId(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePictureUrl(),
                user.getMustChangePassword(),
                user.getUserType() != null ? user.getUserType().name() : null
        );
    }
}
