package com.basilcode.emsbackend.expense.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseClaimDto(
        UUID id,
        UUID employeeId,
        String employeeName,
        String category,
        BigDecimal amount,
        String description,
        String receiptReference,
        String status,
        UUID reviewedById,
        String reviewedByName,
        String reviewNotes,
        OffsetDateTime createdAt,
        OffsetDateTime reviewedAt,
        OffsetDateTime paidAt
) {
}
