package com.basilcode.emsbackend.announcement.entity;

import com.basilcode.emsbackend.announcement.enums.AnnouncementAudience;
import com.basilcode.emsbackend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A company-wide broadcast post — distinct from the Board Room chat (board-member-only) and
 * per-user {@code Notification}s (informational, not composed content). Posting one also creates
 * a SYSTEM notification for every recipient in {@link #targetAudiences}, so it surfaces in the
 * existing notification bell too — see {@code AnnouncementService#create}. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private boolean pinned = false;

    /** Empty is treated the same as containing only {@code ALL} — see
     * {@code AnnouncementService#resolveAudiences}. A row always has at least one entry once
     * persisted through the service; empty only happens for pre-migration legacy rows. */
    @ElementCollection(targetClass = AnnouncementAudience.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "announcement_audiences", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "audience", nullable = false)
    private Set<AnnouncementAudience> targetAudiences = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Null means it never expires — still shows in the active list indefinitely. */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
