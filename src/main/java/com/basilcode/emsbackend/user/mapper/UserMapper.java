package com.basilcode.emsbackend.user.mapper;

import com.basilcode.emsbackend.user.dto.UserResponseData;
import com.basilcode.emsbackend.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseData userResponseMapper(User user);
}
