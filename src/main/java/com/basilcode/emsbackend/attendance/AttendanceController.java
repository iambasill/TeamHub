package com.basilcode.emsbackend.attendance;

import com.basilcode.emsbackend.attendance.dto.AttendanceReportEntryResponse;
import com.basilcode.emsbackend.attendance.dto.AttendanceResponse;
import com.basilcode.emsbackend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceServices attendanceServices;

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(@AuthenticationPrincipal UserDetails currentUser) {
        AttendanceResponse response = attendanceServices.checkIn(currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Checked in successfully", response));
    }

    @PutMapping("/check-out")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(@AuthenticationPrincipal UserDetails currentUser) {
        AttendanceResponse response = attendanceServices.checkOut(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Checked out successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMyAttendance(
            @AuthenticationPrincipal UserDetails currentUser) {
        List<AttendanceResponse> responses = attendanceServices.getMyAttendance(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Attendance records retrieved successfully", responses));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAllAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID departmentId) {
        List<AttendanceResponse> responses = attendanceServices.getAllAttendance(date, departmentId);
        return ResponseEntity.ok(ApiResponse.success("Attendance records retrieved successfully", responses));
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceReportEntryResponse>>> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {
        List<AttendanceReportEntryResponse> responses = attendanceServices.getMonthlyReport(YearMonth.of(year, month));
        return ResponseEntity.ok(ApiResponse.success("Monthly attendance report retrieved successfully", responses));
    }
}
