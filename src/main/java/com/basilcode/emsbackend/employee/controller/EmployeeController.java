package com.basilcode.emsbackend.employee.controller;

import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.employee.dto.EmployeeQuerySearch;
import com.basilcode.emsbackend.employee.dto.EmployeeRequest;
import com.basilcode.emsbackend.employee.dto.EmployeeResponse;
import com.basilcode.emsbackend.employee.dto.EmployeeUpdateRequest;
import com.basilcode.emsbackend.employee.dto.ProfilePictureRequest;
import com.basilcode.emsbackend.employee.service.IEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final IEmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody EmployeeRequest employeeRequest) {
        EmployeeResponse response = employeeService.createEmployee(employeeRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails currentUser) {
        EmployeeResponse response = employeeService.getEmployee(id, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees(
            @ModelAttribute EmployeeQuerySearch query, @AuthenticationPrincipal UserDetails currentUser) {
        List<EmployeeResponse> responses = employeeService.getAllEmployees(query, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", responses));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails currentUser) {
        EmployeeResponse response = employeeService.getEmployeeByUserEmail(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    @PostMapping(value = "/me/profile-picture", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateMyProfilePicture(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam("file") MultipartFile file) {
        try {
            EmployeeResponse response = employeeService.updateMyProfilePicture(currentUser.getUsername(), file);
            return ResponseEntity.ok(ApiResponse.success("Profile picture updated successfully", response));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to upload profile picture", e);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateRequest employeeRequest) {
        EmployeeResponse response = employeeService.updateEmployee(id, employeeRequest);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    /** Soft-deactivates the employee record and locks their login — see
     * {@code EmployeeServices.deleteEmployee} for why both happen together. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activateEmployee(@PathVariable UUID id) {
        EmployeeResponse response = employeeService.activateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee reactivated successfully", response));
    }
}