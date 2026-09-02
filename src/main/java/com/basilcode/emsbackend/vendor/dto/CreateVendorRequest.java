package com.basilcode.emsbackend.vendor.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVendorRequest(
        @NotBlank String name,
        String category,
        String contactName,
        String contactEmail,
        String contactPhone,
        String notes
) {
}
