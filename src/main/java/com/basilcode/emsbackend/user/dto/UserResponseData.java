package com.basilcode.emsbackend.user.dto;

import com.basilcode.emsbackend.user.entity.User;
import lombok.Data;

import java.util.Optional;

@Data

public class UserResponseData {
    String Id;
    String firstName;
    String lastName;
    String emailId;
}
