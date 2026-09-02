package com.basilcode.emsbackend.policy.entity;

import com.basilcode.emsbackend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A persistent, searchable reference document (policy/SOP) — distinct from Board Room documents
 * (board-member-only) and Announcements (time-boxed broadcast posts, not reference material).
 * {@code fileUrl} is an internal storage detail, same discipline as {@code BoardDocument} — never
 * exposed via DTO, files are always fetched through the download endpoint. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "policy_documents")
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    @PrePersist
    public void onCreate() {
        uploadedAt = OffsetDateTime.now();
    }
}
