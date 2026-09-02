package com.basilcode.emsbackend.incident.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentDto(
        UUID id,
        String title,
        String description,
        String severity,
        String status,
        String source,
        String sourceRef,
        UUID assignedToId,
        String assignedToName,
        String createdByName,
        OffsetDateTime createdAt,
        OffsetDateTime acknowledgedAt,
        OffsetDateTime resolvedAt,
        String resolutionNotes,
        int slaMinutes,
        boolean slaBreached
) {
}
