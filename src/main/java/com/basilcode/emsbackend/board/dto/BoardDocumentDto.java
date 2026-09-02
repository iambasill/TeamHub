package com.basilcode.emsbackend.board.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BoardDocumentDto(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        String uploadedByName,
        OffsetDateTime uploadedAt,
        boolean isRecording
) {
}
