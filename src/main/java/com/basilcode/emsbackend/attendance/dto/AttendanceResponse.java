package com.basilcode.emsbackend.attendance.dto;

import com.basilcode.emsbackend.attendance.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceResponse {
    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private UUID departmentId;
    private String departmentName;
    private OffsetDateTime checkIn;
    private OffsetDateTime checkOut;
    private LocalDate date;
    private AttendanceStatus status;
}
