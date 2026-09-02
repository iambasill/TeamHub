package com.basilcode.emsbackend.user.service;

import com.basilcode.emsbackend.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

public interface IUser {
    Optional<User> getUserById(UUID id);
    Optional<User> findUserByEmail(String email);
    boolean existsByEmail(String email);
    void createUser(User user);

}