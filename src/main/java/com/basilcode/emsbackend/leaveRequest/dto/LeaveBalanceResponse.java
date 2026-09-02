package com.basilcode.emsbackend.leaveRequest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaveBalanceResponse {
    private int entitlementDays;
    private int usedDays;
    private int remainingDays;
}
