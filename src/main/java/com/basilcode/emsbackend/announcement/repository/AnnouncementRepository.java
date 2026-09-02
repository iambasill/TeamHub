package com.basilcode.emsbackend.announcement.repository;

import com.basilcode.emsbackend.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.expiresAt IS NULL OR a.expiresAt > :now
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    List<Announcement> findActive(@Param("now") OffsetDateTime now);
}
