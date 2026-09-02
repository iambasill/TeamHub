package com.basilcode.emsbackend.employee.repository;

import com.basilcode.emsbackend.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByUser_EmailId(String email);
    List<Employee> findByActiveTrue();
}
