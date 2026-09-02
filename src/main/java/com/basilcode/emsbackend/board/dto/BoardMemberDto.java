package com.basilcode.emsbackend.board.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BoardMemberDto(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String userType,
        String addedByName,
        OffsetDateTime addedAt,
        boolean active,
        OffsetDateTime lastActivityAt
) {
}
