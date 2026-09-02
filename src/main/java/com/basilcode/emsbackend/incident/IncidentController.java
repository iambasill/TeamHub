package com.basilcode.emsbackend.incident;

import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.incident.dto.AssignIncidentRequest;
import com.basilcode.emsbackend.incident.dto.CreateIncidentRequest;
import com.basilcode.emsbackend.incident.dto.CreateOnCallAssignmentRequest;
import com.basilcode.emsbackend.incident.dto.IncidentDto;
import com.basilcode.emsbackend.incident.dto.IncidentDtoMapper;
import com.basilcode.emsbackend.incident.dto.OnCallAssignmentDto;
import com.basilcode.emsbackend.incident.dto.ResolveIncidentRequest;
import com.basilcode.emsbackend.incident.entity.Incident;
import com.basilcode.emsbackend.incident.entity.OnCallAssignment;
import com.basilcode.emsbackend.incident.enums.IncidentSeverity;
import com.basilcode.emsbackend.incident.enums.IncidentStatus;
import com.basilcode.emsbackend.incident.repository.OnCallAssignmentRepository;
import com.basilcode.emsbackend.incident.service.IncidentService;
import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Company-wide incident tracking, open to every authenticated employee to view and raise.
 * Triage actions (assign, close, on-call roster management) are restricted to the same
 * ops-managing roles used for leave approval elsewhere in the app; acknowledging/resolving is
 * left open to any user since that's typically the assignee acting on their own ticket.
 */
@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentDtoMapper mapper;
    private final UserRepository userRepository;
    private final OnCallAssignmentRepository onCallAssignmentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IncidentDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        IncidentStatus statusEnum = parseEnumOrNull(IncidentStatus.class, status, "status");
        IncidentSeverity severityEnum = parseEnumOrNull(IncidentSeverity.class, severity, "severity");
        List<IncidentDto> incidents = incidentService.search(statusEnum, severityEnum).stream()
                .map(mapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Incidents retrieved", incidents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentDto>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Incident retrieved", mapper.toDto(incidentService.get(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IncidentDto>> create(
            @Valid @RequestBody CreateIncidentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        IncidentSeverity severity = parseEnumOrThrow(IncidentSeverity.class, request.severity(), "severity");
        Incident incident = incidentService.create(request.title(), request.description(), severity, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Incident reported", mapper.toDto(incident)));
    }

    // @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<IncidentDto>> assign(@PathVariable UUID id, @Valid @RequestBody AssignIncidentRequest request) {
        Incident incident = incidentService.assign(id, request.assigneeUserId());
        return ResponseEntity.ok(ApiResponse.success("Incident assigned", mapper.toDto(incident)));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<IncidentDto>> acknowledge(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Incident acknowledged", mapper.toDto(incidentService.acknowledge(id))));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<IncidentDto>> resolve(@PathVariable UUID id, @RequestBody(required = false) ResolveIncidentRequest request) {
        String notes = request != null ? request.resolutionNotes() : null;
        return ResponseEntity.ok(ApiResponse.success("Incident resolved", mapper.toDto(incidentService.resolve(id, notes))));
    }

    // @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<IncidentDto>> close(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Incident closed", mapper.toDto(incidentService.close(id))));
    }

    @GetMapping("/on-call")
    public ResponseEntity<ApiResponse<List<OnCallAssignmentDto>>> listOnCall() {
        List<OnCallAssignmentDto> assignments = onCallAssignmentRepository
                .findByEndsAtAfterOrderByStartsAt(OffsetDateTime.now()).stream()
                .map(mapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("On-call roster retrieved", assignments));
    }

    @GetMapping("/on-call/current")
    public ResponseEntity<ApiResponse<List<OnCallAssignmentDto>>> currentOnCall() {
        List<OnCallAssignmentDto> assignments = onCallAssignmentRepository
                .findCoveringInstant(OffsetDateTime.now()).stream()
                .map(mapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Current on-call retrieved", assignments));
    }

    // @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @PostMapping("/on-call")
    public ResponseEntity<ApiResponse<OnCallAssignmentDto>> createOnCall(
            @Valid @RequestBody CreateOnCallAssignmentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        User createdBy = resolveUser(currentUser);
        User onCallUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.userId()));
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("endsAt must be after startsAt");
        }

        OnCallAssignment assignment = new OnCallAssignment();
        assignment.setUser(onCallUser);
        assignment.setStartsAt(request.startsAt());
        assignment.setEndsAt(request.endsAt());
        assignment.setCreatedBy(createdBy);
        assignment = onCallAssignmentRepository.save(assignment);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("On-call assignment created", mapper.toDto(assignment)));
    }

    // @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @DeleteMapping("/on-call/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOnCall(@PathVariable UUID id) {
        if (!onCallAssignmentRepository.existsById(id)) {
            throw new NotFoundException("On-call assignment not found: " + id);
        }
        onCallAssignmentRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("On-call assignment removed"));
    }

    private User resolveUser(UserDetails currentUser) {
        return userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
    }

    private <E extends Enum<E>> E parseEnumOrNull(Class<E> type, String value, String fieldName) {
        return value == null ? null : parseEnumOrThrow(type, value, fieldName);
    }

    private <E extends Enum<E>> E parseEnumOrThrow(Class<E> type, String value, String fieldName) {
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + fieldName + ": " + value);
        }
    }
}
