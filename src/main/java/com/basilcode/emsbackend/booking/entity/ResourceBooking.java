package com.basilcode.emsbackend.booking.entity;

import com.basilcode.emsbackend.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single time-slot reservation of a {@link Resource}. Soft-cancelled via {@code cancelledAt}
 * rather than deleted, so booking history (and past conflict data) survives a cancellation.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resource_bookings")
public class ResourceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by", nullable = false)
    private Employee bookedBy;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Null while active; set to the cancellation time on soft-cancel. */
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
