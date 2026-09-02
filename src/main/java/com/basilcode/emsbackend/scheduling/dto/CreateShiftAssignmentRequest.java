package com.basilcode.emsbackend.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateShiftAssignmentRequest(
        @NotNull UUID shiftId,
        @NotNull UUID employeeId,
        @NotNull LocalDate date
) {
}
