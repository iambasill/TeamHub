package com.basilcode.emsbackend.expense.entity;

import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.expense.enums.ExpenseCategory;
import com.basilcode.emsbackend.expense.enums.ExpenseStatus;
import com.basilcode.emsbackend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "expense_claims")
public class ExpenseClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "text")
    private String description;

    /** e.g. a filename or note pointing at the receipt — no actual file upload for this feature. */
    @Column(name = "receipt_reference")
    private String receiptReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_notes", columnDefinition = "text")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) {
            status = ExpenseStatus.PENDING;
        }
    }
}
