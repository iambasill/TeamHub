package com.basilcode.emsbackend.board.entity;

import com.basilcode.emsbackend.board.enums.BoardActivityType;
import com.basilcode.emsbackend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Every board entry and every significant board-scoped action (a ticket decision, a triggered
 * pipeline run, etc.) — the audit trail members and admins can review. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "board_activity_log")
public class BoardActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private BoardActivityType activityType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @PrePersist
    public void onCreate() {
        occurredAt = OffsetDateTime.now();
    }
}
