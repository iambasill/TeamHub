package com.basilcode.emsbackend.vendor.dto;

import java.util.Date;
import java.util.UUID;

public record VendorDto(
        UUID id,
        String name,
        String category,
        String contactName,
        String contactEmail,
        String contactPhone,
        String notes,
        String status,
        Date createdAt,
        Date updatedAt
) {
}
