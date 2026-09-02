package com.basilcode.emsbackend.opspulse.service;

import com.basilcode.emsbackend.announcement.repository.AnnouncementRepository;
import com.basilcode.emsbackend.asset.enums.AssetStatus;
import com.basilcode.emsbackend.asset.repository.AssetRepository;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import com.basilcode.emsbackend.incident.entity.Incident;
import com.basilcode.emsbackend.incident.enums.IncidentSeverity;
import com.basilcode.emsbackend.incident.enums.IncidentStatus;
import com.basilcode.emsbackend.incident.repository.IncidentRepository;
import com.basilcode.emsbackend.incident.repository.OnCallAssignmentRepository;
import com.basilcode.emsbackend.leaveRequest.repository.LeaveRequestRepository;
import com.basilcode.emsbackend.opspulse.dto.OpsPulseDto;
import com.basilcode.emsbackend.scheduling.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A single "company pulse" overview aggregating headline numbers from several independently-built
 * ops modules (Incidents, Scheduling, Assets, Announcements, Leave, Employees) that otherwise only
 * exist as separate pages. Read-only, no entity/table of its own. Each figure is computed
 * defensively — a problem reading one source degrades that one number to zero rather than failing
 * the whole dashboard, since this deliberately spans modules that evolve somewhat independently.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpsPulseService {

    /** Mirrors {@code IncidentDtoMapper.SLA_MINUTES} — duplicated rather than reached into, since
     * that map is private to the mapper and this is a read-only summary count, not a shared
     * business rule that needs a single source of truth beyond "same numbers, same intent." */
    private static final Map<IncidentSeverity, Integer> SLA_MINUTES = Map.of(
            IncidentSeverity.CRITICAL, 15,
            IncidentSeverity.HIGH, 60,
            IncidentSeverity.MEDIUM, 240,
            IncidentSeverity.LOW, 1440
    );

    private final EmployeeRepository employeeRepository;
    private final SchedulingService schedulingService;
    private final IncidentRepository incidentRepository;
    private final OnCallAssignmentRepository onCallAssignmentRepository;
    private final AssetRepository assetRepository;
    private final AnnouncementRepository announcementRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public OpsPulseDto buildPulse() {
        return new OpsPulseDto(
                safe("activeEmployeeCount", () -> employeeRepository.findByActiveTrue().size()),
                safe("currentlyOnDutyCount", () -> schedulingService.currentlyOnDuty().size()),
                safe("openIncidentCount", this::countOpenIncidents),
                safe("slaBreachedIncidentCount", this::countSlaBreachedIncidents),
                safe("currentlyOnCallCount", () -> onCallAssignmentRepository.findCoveringInstant(OffsetDateTime.now()).size()),
                safe("assetsInMaintenanceCount", this::countAssetsInMaintenance),
                safe("activeAnnouncementCount", () -> announcementRepository.findActive(OffsetDateTime.now()).size()),
                safe("pendingLeaveRequestCount", this::countPendingLeaveRequests));
    }

    private int countOpenIncidents() {
        return (int) incidentRepository.findAll().stream()
                .filter(i -> i.getStatus() == IncidentStatus.OPEN || i.getStatus() == IncidentStatus.ACKNOWLEDGED)
                .count();
    }

    private int countSlaBreachedIncidents() {
        OffsetDateTime now = OffsetDateTime.now();
        return (int) incidentRepository.findAll().stream()
                .filter(i -> i.getStatus() == IncidentStatus.OPEN || i.getStatus() == IncidentStatus.ACKNOWLEDGED)
                .filter(i -> isBreached(i, now))
                .count();
    }

    private boolean isBreached(Incident incident, OffsetDateTime now) {
        int slaMinutes = SLA_MINUTES.get(incident.getSeverity());
        return now.isAfter(incident.getCreatedAt().plusMinutes(slaMinutes));
    }

    private int countAssetsInMaintenance() {
        return (int) assetRepository.findAllByOrderByNameAsc().stream()
                .filter(a -> a.getStatus() == AssetStatus.MAINTENANCE)
                .count();
    }

    private int countPendingLeaveRequests() {
        return (int) leaveRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .count();
    }

    /** Isolates one figure's computation so a problem reading one source zeroes only that number. */
    private int safe(String metricName, Supplier<Integer> computation) {
        try {
            return computation.get();
        } catch (Exception e) {
            log.warn("Ops pulse: failed to compute {}, defaulting to 0: {}", metricName, e.getMessage());
            return 0;
        }
    }
}
