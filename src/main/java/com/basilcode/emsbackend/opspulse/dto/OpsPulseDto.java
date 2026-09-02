package com.basilcode.emsbackend.opspulse.dto;

public record OpsPulseDto(
        int activeEmployeeCount,
        int currentlyOnDutyCount,
        int openIncidentCount,
        int slaBreachedIncidentCount,
        int currentlyOnCallCount,
        int assetsInMaintenanceCount,
        int activeAnnouncementCount,
        int pendingLeaveRequestCount
) {
}
