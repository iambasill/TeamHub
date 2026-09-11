package com.basilcode.emsbackend.employee.service;

import com.basilcode.emsbackend.employee.dto.EmployeeQuerySearch;
import com.basilcode.emsbackend.employee.dto.EmployeeRequest;
import com.basilcode.emsbackend.employee.dto.EmployeeResponse;
import com.basilcode.emsbackend.employee.dto.EmployeeUpdateRequest;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;


public interface IEmployeeService {
    EmployeeResponse createEmployee(EmployeeRequest employeeRequest);
    List<EmployeeResponse> getAllEmployees(EmployeeQuerySearch query, String requesterEmail);
    EmployeeResponse getEmployee(UUID id, String requesterEmail);
    EmployeeResponse updateEmployee(UUID id, EmployeeUpdateRequest employeeRequest);
    void deleteEmployee(UUID id);
    EmployeeResponse activateEmployee(UUID id);
    EmployeeResponse getEmployeeByUserEmail(String email);
    EmployeeResponse updateMyProfilePicture(String email, MultipartFile file) throws IOException;
}
