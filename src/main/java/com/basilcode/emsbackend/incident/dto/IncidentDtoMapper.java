package com.basilcode.emsbackend.incident.dto;

import com.basilcode.emsbackend.incident.entity.Incident;
import com.basilcode.emsbackend.incident.entity.OnCallAssignment;
import com.basilcode.emsbackend.incident.enums.IncidentSeverity;
import com.basilcode.emsbackend.incident.enums.IncidentStatus;
import com.basilcode.emsbackend.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class IncidentDtoMapper {

    /** How long an OPEN/ACKNOWLEDGED incident can sit before it's flagged overdue — informational
     * only: it never auto-escalates or notifies anyone, it just makes the overdue state visible
     * on the dashboard. */
    private static final Map<IncidentSeverity, Integer> SLA_MINUTES = Map.of(
            IncidentSeverity.CRITICAL, 15,
            IncidentSeverity.HIGH, 60,
            IncidentSeverity.MEDIUM, 240,
            IncidentSeverity.LOW, 1440
    );

    public IncidentDto toDto(Incident incident) {
        int slaMinutes = SLA_MINUTES.get(incident.getSeverity());
        boolean unresolved = incident.getStatus() == IncidentStatus.OPEN || incident.getStatus() == IncidentStatus.ACKNOWLEDGED;
        boolean breached = unresolved
                && OffsetDateTime.now().isAfter(incident.getCreatedAt().plusMinutes(slaMinutes));

        return new IncidentDto(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity().name(),
                incident.getStatus().name(),
                incident.getSource().name(),
                incident.getSourceRef(),
                incident.getAssignedTo() != null ? incident.getAssignedTo().getId() : null,
                fullName(incident.getAssignedTo()),
                fullName(incident.getCreatedBy()),
                incident.getCreatedAt(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                incident.getResolutionNotes(),
                slaMinutes,
                breached);
    }

    public OnCallAssignmentDto toDto(OnCallAssignment assignment) {
        return new OnCallAssignmentDto(
                assignment.getId(),
                assignment.getUser().getId(),
                fullName(assignment.getUser()),
                assignment.getStartsAt(),
                assignment.getEndsAt(),
                fullName(assignment.getCreatedBy()),
                assignment.getCreatedAt());
    }

    private String fullName(User user) {
        return user == null ? null : user.getFirstName() + " " + user.getLastName();
    }
}
