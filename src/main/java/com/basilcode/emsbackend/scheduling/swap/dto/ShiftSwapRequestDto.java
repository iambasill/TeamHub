package com.basilcode.emsbackend.scheduling.swap.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShiftSwapRequestDto(
        UUID id,
        UUID shiftAssignmentId,
        String shiftName,
        LocalDate shiftDate,
        UUID requestedByEmployeeId,
        String requestedByName,
        UUID proposedToEmployeeId,
        String proposedToName,
        String status,
        String reason,
        OffsetDateTime createdAt,
        OffsetDateTime decidedAt,
        String decidedByName
) {
}
