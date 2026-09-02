package com.basilcode.emsbackend.asset.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignAssetRequest(@NotNull UUID employeeId) {
}
