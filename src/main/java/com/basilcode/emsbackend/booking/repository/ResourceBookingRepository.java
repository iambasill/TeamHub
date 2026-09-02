package com.basilcode.emsbackend.booking.repository;

import com.basilcode.emsbackend.booking.entity.ResourceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ResourceBookingRepository extends JpaRepository<ResourceBooking, UUID>,
        JpaSpecificationExecutor<ResourceBooking> {

    /**
     * Standard interval-overlap query used for conflict-checking: an existing, non-cancelled
     * booking overlaps a proposed [proposedStart, proposedEnd) slot exactly when
     * {@code existing.startsAt < proposedEnd AND existing.endsAt > proposedStart}.
     */
    List<ResourceBooking> findByResource_IdAndCancelledAtIsNullAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID resourceId, OffsetDateTime proposedEnd, OffsetDateTime proposedStart);
}
