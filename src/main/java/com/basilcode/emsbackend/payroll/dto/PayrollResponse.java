package com.basilcode.emsbackend.payroll.dto;

import com.basilcode.emsbackend.payroll.enums.PayrollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayrollResponse {
    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private LocalDate payPeriod;
    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;
    private BigDecimal netSalary;
    private PayrollStatus status;
    private OffsetDateTime processedAt;
    private OffsetDateTime createdAt;
}
