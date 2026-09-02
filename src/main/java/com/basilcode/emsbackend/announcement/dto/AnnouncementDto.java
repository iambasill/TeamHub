package com.basilcode.emsbackend.announcement.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AnnouncementDto(
        UUID id,
        String title,
        String body,
        boolean pinned,
        List<String> targetAudiences,
        String createdByName,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {
}
