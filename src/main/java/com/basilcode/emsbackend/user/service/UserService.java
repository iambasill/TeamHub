package com.basilcode.emsbackend.user.service;

import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.mapper.UserMapper;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService implements IUser, UserDetailsService {
    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
            return userRepository.findByEmailId(email);
        }


    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailId(email);
    }

    @Override
    public void createUser(User user) {
        userRepository.save(user);

    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }


}
