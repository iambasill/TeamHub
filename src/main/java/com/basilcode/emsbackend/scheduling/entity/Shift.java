package com.basilcode.emsbackend.scheduling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A reusable shift definition (e.g. "Night", 22:00-06:00) — the actual roster is built by
 * assigning employees to a shift on specific dates via {@link ShiftAssignment}. Overnight shifts
 * (endTime before startTime) are valid and handled explicitly wherever "is this shift active now"
 * is computed. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    /** True if a shift starting at {@link #startTime} and ending at {@link #endTime} crosses
     * midnight (e.g. 22:00-06:00) rather than falling entirely within one calendar day. */
    @Transient
    public boolean isOvernight() {
        return endTime.isBefore(startTime);
    }
}
