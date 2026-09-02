package com.basilcode.emsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceReportEntryResponse {
    private UUID employeeId;
    private String employeeName;
    private long presentDays;
    private long absentDays;
    private long lateDays;
    private long halfDays;
    private long onLeaveDays;
    private long totalRecordedDays;
}
