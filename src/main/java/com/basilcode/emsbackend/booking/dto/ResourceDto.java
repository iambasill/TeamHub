package com.basilcode.emsbackend.booking.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResourceDto(
        UUID id,
        String name,
        String description,
        Integer capacity,
        boolean active,
        OffsetDateTime createdAt
) {
}
