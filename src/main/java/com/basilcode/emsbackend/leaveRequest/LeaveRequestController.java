package com.basilcode.emsbackend.leaveRequest;

import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.leaveRequest.dto.LeaveHistoryResponse;
import com.basilcode.emsbackend.leaveRequest.dto.LeaveRequestCreateDto;
import com.basilcode.emsbackend.leaveRequest.dto.LeaveRequestResponse;
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
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestServices leaveRequestServices;

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> submitLeaveRequest(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody LeaveRequestCreateDto request) {
        LeaveRequestResponse response = leaveRequestServices.submitLeaveRequest(currentUser.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave request submitted successfully", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LeaveHistoryResponse>> getMyLeaveHistory(
            @AuthenticationPrincipal UserDetails currentUser) {
        LeaveHistoryResponse response = leaveRequestServices.getMyLeaveHistory(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Leave history retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getAllLeaveRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID employeeId) {
        List<LeaveRequestResponse> responses = leaveRequestServices.getAllLeaveRequests(status, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Leave requests retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getLeaveRequest(@PathVariable UUID id) {
        LeaveRequestResponse response = leaveRequestServices.getLeaveRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Leave request retrieved successfully", response));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approveLeaveRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        LeaveRequestResponse response = leaveRequestServices.approveLeaveRequest(id, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Leave request approved successfully", response));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> rejectLeaveRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        LeaveRequestResponse response = leaveRequestServices.rejectLeaveRequest(id, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Leave request rejected successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOwnLeaveRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        leaveRequestServices.cancelOwnLeaveRequest(id, currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }
}
