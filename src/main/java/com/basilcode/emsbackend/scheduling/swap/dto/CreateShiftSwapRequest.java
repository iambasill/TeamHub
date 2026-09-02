package com.basilcode.emsbackend.scheduling.swap.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** {@code proposedToEmployeeId} null means "anyone" — an admin/manager picks the replacement
 * when approving (see {@code ApproveShiftSwapRequest#replacementEmployeeId}). */
public record CreateShiftSwapRequest(
        @NotNull UUID shiftAssignmentId,
        UUID proposedToEmployeeId,
        String reason
) {
}
