package com.basilcode.emsbackend.attendance.repository;

import com.basilcode.emsbackend.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByEmployee_IdAndDate(UUID employeeId, LocalDate date);
    List<Attendance> findByEmployee_IdOrderByDateDesc(UUID employeeId);
    List<Attendance> findByDateBetweenOrderByDateDesc(LocalDate from, LocalDate to);
}
