package com.basilcode.emsbackend.board.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BoardActivityLogDto(
        UUID id,
        UUID userId,
        String userName,
        String activityType,
        String description,
        OffsetDateTime occurredAt
) {
}
