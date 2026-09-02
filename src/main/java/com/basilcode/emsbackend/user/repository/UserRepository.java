package com.basilcode.emsbackend.user.repository;

import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailId(String email);
    boolean existsByEmailId(String email);
    List<User> findByUserTypeIn(List<UserTypeEnum> userTypes);
}

