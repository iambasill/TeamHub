package com.basilcode.emsbackend.payroll.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PayrollRunRequest {

    @NotNull(message = "Pay period is required")
    private LocalDate payPeriod;
}
