package com.basilcode.emsbackend.incident.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateIncidentRequest(
        @NotBlank String title,
        String description,
        @NotBlank String severity
) {
}
