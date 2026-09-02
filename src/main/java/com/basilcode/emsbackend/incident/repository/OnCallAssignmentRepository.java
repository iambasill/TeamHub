package com.basilcode.emsbackend.incident.repository;

import com.basilcode.emsbackend.incident.entity.OnCallAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OnCallAssignmentRepository extends JpaRepository<OnCallAssignment, UUID> {

    @Query("SELECT o FROM OnCallAssignment o WHERE :at BETWEEN o.startsAt AND o.endsAt ORDER BY o.startsAt")
    List<OnCallAssignment> findCoveringInstant(@Param("at") OffsetDateTime at);

    List<OnCallAssignment> findByEndsAtAfterOrderByStartsAt(OffsetDateTime after);
}
