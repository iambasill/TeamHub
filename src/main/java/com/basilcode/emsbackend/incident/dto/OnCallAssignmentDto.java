package com.basilcode.emsbackend.incident.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OnCallAssignmentDto(
        UUID id,
        UUID userId,
        String userName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String createdByName,
        OffsetDateTime createdAt
) {
}
