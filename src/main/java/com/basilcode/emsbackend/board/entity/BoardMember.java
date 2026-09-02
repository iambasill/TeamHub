package com.basilcode.emsbackend.board.entity;

import com.basilcode.emsbackend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Board Room access is membership-based, not role-based — a row here is what grants access,
 * independent of {@code UserTypeEnum}. An ADMIN/SUPER_ADMIN user with no row here has no board
 * access; a non-admin user with a row here does.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "board_members")
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

    @Column(name = "added_at", nullable = false, updatable = false)
    private OffsetDateTime addedAt;

    @PrePersist
    public void onCreate() {
        addedAt = OffsetDateTime.now();
    }
}
