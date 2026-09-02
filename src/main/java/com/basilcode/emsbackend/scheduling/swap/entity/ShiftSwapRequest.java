package com.basilcode.emsbackend.scheduling.swap.entity;

import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.scheduling.entity.ShiftAssignment;
import com.basilcode.emsbackend.scheduling.swap.enums.ShiftSwapStatus;
import com.basilcode.emsbackend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A request by the employee currently on a {@link ShiftAssignment} to hand it off to someone
 * else — either a specific coworker ({@link #proposedTo}) or, if null, "anyone", left for an
 * admin to resolve by picking a replacement at approval time. Approving the request actually
 * reassigns the underlying {@code ShiftAssignment.employee}; it does not create a new assignment.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shift_swap_requests")
public class ShiftSwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_assignment_id", nullable = false)
    private ShiftAssignment shiftAssignment;

    /** The employee giving up the shift — must be the employee currently on the assignment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private Employee requestedBy;

    /** Null means "anyone" — an admin/manager picks the replacement when approving. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_to")
    private Employee proposedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftSwapStatus status = ShiftSwapStatus.PENDING;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    /** Whoever actioned the approve/reject — either the proposed coworker or a manager/admin. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) {
            status = ShiftSwapStatus.PENDING;
        }
    }
}
