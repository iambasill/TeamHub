package com.basilcode.emsbackend.asset.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssetDto(
        UUID id,
        String name,
        String category,
        String serialNumber,
        String status,
        String notes,
        UUID currentHolderEmployeeId,
        String currentHolderName,
        OffsetDateTime assignedAt
) {
}
