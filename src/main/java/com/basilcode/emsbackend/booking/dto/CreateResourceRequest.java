package com.basilcode.emsbackend.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateResourceRequest(
        @NotBlank String name,
        String description,
        Integer capacity
) {
}
