package com.basilcode.emsbackend.booking.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResourceBookingDto(
        UUID id,
        UUID resourceId,
        String resourceName,
        UUID bookedByEmployeeId,
        String bookedByName,
        String title,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime createdAt,
        OffsetDateTime cancelledAt
) {
}
