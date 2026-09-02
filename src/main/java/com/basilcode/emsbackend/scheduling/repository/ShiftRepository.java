package com.basilcode.emsbackend.scheduling.repository;

import com.basilcode.emsbackend.scheduling.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    List<Shift> findAllByOrderByStartTime();
}
