package com.basilcode.emsbackend.incident.service;

import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.incident.entity.Incident;
import com.basilcode.emsbackend.incident.enums.IncidentSeverity;
import com.basilcode.emsbackend.incident.enums.IncidentSource;
import com.basilcode.emsbackend.incident.enums.IncidentStatus;
import com.basilcode.emsbackend.incident.repository.IncidentRepository;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Company-wide operational incidents. Everyone can see and raise an incident; only
 * HR/ADMIN/SUPER_ADMIN/MANAGER can assign or formally close one, enforced in
 * {@code IncidentController}, not here.
 */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Incident create(String title, String description, IncidentSeverity severity, User createdBy) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription(description);
        incident.setSeverity(severity);
        incident.setSource(IncidentSource.MANUAL);
        incident.setCreatedBy(createdBy);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident assign(UUID incidentId, UUID assigneeUserId) {
        Incident incident = get(incidentId);
        User assignee = userRepository.findById(assigneeUserId)
                .orElseThrow(() -> new NotFoundException("User not found: " + assigneeUserId));
        incident.setAssignedTo(assignee);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident acknowledge(UUID incidentId) {
        Incident incident = get(incidentId);
        if (incident.getStatus() != IncidentStatus.OPEN) {
            throw new BadRequestException("Only an OPEN incident can be acknowledged (current: " + incident.getStatus() + ").");
        }
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(OffsetDateTime.now());
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident resolve(UUID incidentId, String resolutionNotes) {
        Incident incident = get(incidentId);
        if (incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Incident is already " + incident.getStatus() + ".");
        }
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(OffsetDateTime.now());
        incident.setResolutionNotes(resolutionNotes);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident close(UUID incidentId) {
        Incident incident = get(incidentId);
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new BadRequestException("Only a RESOLVED incident can be closed (current: " + incident.getStatus() + ").");
        }
        incident.setStatus(IncidentStatus.CLOSED);
        return incidentRepository.save(incident);
    }

    // A Specification (not a JPQL "?1 IS NULL OR ..." filter) so an unset filter is simply never
    // bound as a parameter — Postgres cannot infer a type for a bind parameter that only ever
    // appears in an "IS NULL" check, which raises a 42P18 "indeterminate datatype" error.
    public List<Incident> search(IncidentStatus status, IncidentSeverity severity) {
        Specification<Incident> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (severity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        return incidentRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Incident get(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + id));
    }
}
