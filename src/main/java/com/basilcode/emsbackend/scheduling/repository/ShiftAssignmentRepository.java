package com.basilcode.emsbackend.scheduling.repository;

import com.basilcode.emsbackend.scheduling.entity.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, UUID> {
    List<ShiftAssignment> findByDateBetweenOrderByDateAscShift_StartTimeAsc(LocalDate start, LocalDate end);
    List<ShiftAssignment> findByDateOrderByShift_StartTimeAsc(LocalDate date);
    boolean existsByShift_IdAndEmployee_IdAndDate(UUID shiftId, UUID employeeId, LocalDate date);
}
