package com.basilcode.emsbackend.incident.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignIncidentRequest(@NotNull UUID assigneeUserId) {
}
