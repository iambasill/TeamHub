package com.basilcode.emsbackend.announcement.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.List;

/** {@code targetAudiences} null or empty means everyone (equivalent to {@code ["ALL"]}) — see
 * {@code AnnouncementService#resolveAudiences}. Each entry must be a valid
 * {@code AnnouncementAudience} name. */
public record CreateAnnouncementRequest(
        @NotBlank String title,
        @NotBlank String body,
        boolean pinned,
        OffsetDateTime expiresAt,
        List<String> targetAudiences
) {
}
