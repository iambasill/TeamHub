package com.basilcode.emsbackend.incident.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateOnCallAssignmentRequest(
        @NotNull UUID userId,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt
) {
}
