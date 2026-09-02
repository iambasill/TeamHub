package com.basilcode.emsbackend.asset.entity;

import com.basilcode.emsbackend.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** One checkout period of one asset to one employee. {@code returnedAt} null means it's the
 * asset's current holder — history is kept, never overwritten, so past custody is auditable. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "asset_assignments")
public class AssetAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @Column(name = "condition_notes", columnDefinition = "text")
    private String conditionNotes;

    @PrePersist
    public void onCreate() {
        assignedAt = OffsetDateTime.now();
    }
}
