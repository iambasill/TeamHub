package com.basilcode.emsbackend.booking.repository;

import com.basilcode.emsbackend.booking.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    List<Resource> findAllByActiveTrueOrderByName();
}
