package com.basilcode.emsbackend.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateExpenseClaimRequest(
        @NotBlank String category,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String description,
        String receiptReference
) {
}
