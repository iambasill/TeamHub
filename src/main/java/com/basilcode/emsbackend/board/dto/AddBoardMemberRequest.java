package com.basilcode.emsbackend.board.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddBoardMemberRequest(@NotNull UUID userId) {
}
