package com.basilcode.emsbackend.department;

import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.department.dto.DepartmentRequest;
import com.basilcode.emsbackend.department.dto.DepartmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentServices departmentServices;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentServices.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Department created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        List<DepartmentResponse> responses = departmentServices.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.success("Departments retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable UUID id) {
        DepartmentResponse response = departmentServices.getDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("Department retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentServices.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID id) {
        departmentServices.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
