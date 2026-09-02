package com.basilcode.emsbackend.scheduling.swap.repository;

import com.basilcode.emsbackend.scheduling.swap.entity.ShiftSwapRequest;
import com.basilcode.emsbackend.scheduling.swap.enums.ShiftSwapStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShiftSwapRequestRepository extends JpaRepository<ShiftSwapRequest, UUID> {
    List<ShiftSwapRequest> findAllByOrderByCreatedAtDesc();
    List<ShiftSwapRequest> findByStatusOrderByCreatedAtDesc(ShiftSwapStatus status);
    boolean existsByShiftAssignment_IdAndStatus(UUID shiftAssignmentId, ShiftSwapStatus status);
}
