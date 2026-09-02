package com.basilcode.emsbackend.scheduling.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ShiftAssignmentDto(
        UUID id,
        UUID shiftId,
        String shiftName,
        java.time.LocalTime startTime,
        java.time.LocalTime endTime,
        UUID employeeId,
        String employeeName,
        String employeeJobTitle,
        LocalDate date
) {
}
