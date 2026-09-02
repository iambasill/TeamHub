package com.basilcode.emsbackend.incident.entity;

import com.basilcode.emsbackend.incident.enums.IncidentSeverity;
import com.basilcode.emsbackend.incident.enums.IncidentSource;
import com.basilcode.emsbackend.incident.enums.IncidentStatus;
import com.basilcode.emsbackend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSource source = IncidentSource.MANUAL;

    /** Reference to whatever raised this incident, when it wasn't filed directly by a person. */
    @Column(name = "source_ref")
    private String sourceRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) {
            status = IncidentStatus.OPEN;
        }
        if (source == null) {
            source = IncidentSource.MANUAL;
        }
    }
}
