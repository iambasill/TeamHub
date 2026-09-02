package com.basilcode.emsbackend.expense.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewExpenseClaimRequest(
        @NotBlank String decision,
        String reviewNotes
) {
}
