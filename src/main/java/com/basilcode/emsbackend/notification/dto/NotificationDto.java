package com.basilcode.emsbackend.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String title,
        String description,
        String type,
        boolean unread,
        OffsetDateTime createdAt
) {
}
