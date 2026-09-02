package com.basilcode.emsbackend.scheduling.swap.dto;

import java.util.UUID;

/** {@code replacementEmployeeId} is required only when the original request had no
 * {@code proposedToEmployeeId} (an "anyone" request) — the approver picks who actually takes the
 * shift. Ignored when the request already named a specific coworker. */
public record ApproveShiftSwapRequest(UUID replacementEmployeeId) {
}
