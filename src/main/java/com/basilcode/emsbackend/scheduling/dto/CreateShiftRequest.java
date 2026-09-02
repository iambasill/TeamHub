package com.basilcode.emsbackend.scheduling.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateShiftRequest(
        @NotBlank String name,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String description
) {
}
