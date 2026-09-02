package com.basilcode.emsbackend.booking.service;

import com.basilcode.emsbackend.booking.entity.Resource;
import com.basilcode.emsbackend.booking.entity.ResourceBooking;
import com.basilcode.emsbackend.booking.repository.ResourceBookingRepository;
import com.basilcode.emsbackend.booking.repository.ResourceRepository;
import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.exception.UnAuthorizeException;
import com.basilcode.emsbackend.employee.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ResourceRepository resourceRepository;
    private final ResourceBookingRepository resourceBookingRepository;

    @Transactional
    public Resource createResource(String name, String description, Integer capacity) {
        Resource resource = new Resource();
        resource.setName(name);
        resource.setDescription(description);
        resource.setCapacity(capacity);
        return resourceRepository.save(resource);
    }

    public List<Resource> listResources() {
        return resourceRepository.findAllByActiveTrueOrderByName();
    }

    public List<ResourceBooking> search(UUID resourceId, OffsetDateTime start, OffsetDateTime end) {
        // Built as a Specification (rather than a JPQL "?1 IS NULL OR ..." filter) so an absent
        // filter is simply never bound as a parameter — Postgres cannot infer a type for a bind
        // parameter that only ever appears in an "IS NULL" check, which raises a 42P18
        // "indeterminate datatype" error at query time once any of these filters is left unset.
        Specification<ResourceBooking> spec = Specification.where(null);
        if (resourceId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resource").get("id"), resourceId));
        }
        if (start != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endsAt"), start));
        }
        if (end != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startsAt"), end));
        }
        return resourceBookingRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "startsAt"));
    }

    @Transactional
    public ResourceBooking createBooking(UUID resourceId, String title, OffsetDateTime startsAt, OffsetDateTime endsAt, Employee bookedBy) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BadRequestException("endsAt must be after startsAt");
        }
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found: " + resourceId));
        if (!resource.isActive()) {
            throw new BadRequestException("This resource is no longer available for booking.");
        }

        boolean conflict = !resourceBookingRepository
                .findByResource_IdAndCancelledAtIsNullAndStartsAtLessThanAndEndsAtGreaterThan(resourceId, endsAt, startsAt)
                .isEmpty();
        if (conflict) {
            throw new BadRequestException("This resource is already booked for part of that time slot.");
        }

        ResourceBooking booking = new ResourceBooking();
        booking.setResource(resource);
        booking.setBookedBy(bookedBy);
        booking.setTitle(title);
        booking.setStartsAt(startsAt);
        booking.setEndsAt(endsAt);
        return resourceBookingRepository.save(booking);
    }

    @Transactional
    public void cancelBooking(UUID bookingId, Employee requestingEmployee, boolean isManagement) {
        ResourceBooking booking = resourceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        if (booking.getCancelledAt() != null) {
            throw new BadRequestException("This booking is already cancelled.");
        }
        boolean isOwnBooking = booking.getBookedBy().getId().equals(requestingEmployee.getId());
        if (!isOwnBooking && !isManagement) {
            throw new UnAuthorizeException("You can only cancel your own bookings.");
        }
        booking.setCancelledAt(OffsetDateTime.now());
        resourceBookingRepository.save(booking);
    }
}
