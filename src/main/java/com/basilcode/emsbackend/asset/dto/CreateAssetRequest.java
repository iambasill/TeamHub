package com.basilcode.emsbackend.asset.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAssetRequest(
        @NotBlank String name,
        String category,
        String serialNumber,
        String notes
) {
}
