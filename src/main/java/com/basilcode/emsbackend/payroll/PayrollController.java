package com.basilcode.emsbackend.payroll;

import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.payroll.dto.PayrollResponse;
import com.basilcode.emsbackend.payroll.dto.PayrollRunRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollServices payrollServices;

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> runPayroll(@Valid @RequestBody PayrollRunRequest request) {
        List<PayrollResponse> responses = payrollServices.runPayroll(request.getPayPeriod());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payroll processed successfully", responses));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> getAllPayroll() {
        List<PayrollResponse> responses = payrollServices.getAllPayroll();
        return ResponseEntity.ok(ApiResponse.success("Payroll records retrieved successfully", responses));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> getMyPayroll(@AuthenticationPrincipal UserDetails currentUser) {
        List<PayrollResponse> responses = payrollServices.getMyPayroll(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payslips retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<PayrollResponse>> getPayroll(@PathVariable UUID id) {
        PayrollResponse response = payrollServices.getPayroll(id);
        return ResponseEntity.ok(ApiResponse.success("Payroll record retrieved successfully", response));
    }

    @GetMapping("/employee/{empId}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> getPayrollForEmployee(@PathVariable UUID empId) {
        List<PayrollResponse> responses = payrollServices.getPayrollForEmployee(empId);
        return ResponseEntity.ok(ApiResponse.success("Payslips retrieved successfully", responses));
    }
}
