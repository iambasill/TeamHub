package com.basilcode.emsbackend.payroll.repository;

import com.basilcode.emsbackend.payroll.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRepository extends JpaRepository<Payroll, UUID> {
    List<Payroll> findByEmployee_IdOrderByPayPeriodDesc(UUID employeeId);
    List<Payroll> findAllByOrderByPayPeriodDesc();
    Optional<Payroll> findByEmployee_IdAndPayPeriod(UUID employeeId, LocalDate payPeriod);
}
