package com.basilcode.emsbackend.policy.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PolicyDocumentDto(
        UUID id,
        String title,
        String category,
        String description,
        String fileName,
        String contentType,
        long sizeBytes,
        String uploadedByName,
        OffsetDateTime uploadedAt
) {
}
