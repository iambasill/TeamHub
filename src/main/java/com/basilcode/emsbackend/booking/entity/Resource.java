package com.basilcode.emsbackend.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A shared, reservable resource — a meeting room, a projector, a company car. Distinct from the
 * asset module's {@code Asset}: an asset is checked out long-term to one employee, a resource is
 * booked short-term against a specific time slot and can be reserved by anyone, repeatedly.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /** Seats a room holds, or null when capacity isn't meaningful for this resource (e.g. a car). */
    private Integer capacity;

    /** Lets admins retire a resource (stop it from being booked) without deleting its booking history. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
