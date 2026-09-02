package com.basilcode.emsbackend.leaveRequest.repository;

import com.basilcode.emsbackend.leaveRequest.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByEmployee_IdOrderByCreatedAtDesc(UUID employeeId);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
}
