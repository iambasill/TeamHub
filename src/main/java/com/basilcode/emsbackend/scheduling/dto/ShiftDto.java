package com.basilcode.emsbackend.scheduling.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ShiftDto(
        UUID id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        String description,
        boolean overnight
) {
}
