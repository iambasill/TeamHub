package com.basilcode.emsbackend.booking.dto;

import com.basilcode.emsbackend.booking.entity.Resource;
import com.basilcode.emsbackend.booking.entity.ResourceBooking;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class BookingDtoMapper {

    public ResourceDto toDto(Resource resource) {
        return new ResourceDto(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getCapacity(),
                resource.isActive(),
                resource.getCreatedAt());
    }

    public ResourceBookingDto toDto(ResourceBooking booking) {
        return new ResourceBookingDto(
                booking.getId(),
                booking.getResource().getId(),
                booking.getResource().getName(),
                booking.getBookedBy().getId(),
                fullName(booking.getBookedBy()),
                booking.getTitle(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                booking.getCreatedAt(),
                booking.getCancelledAt());
    }

    private String fullName(Employee employee) {
        User user = employee != null ? employee.getUser() : null;
        return user == null ? null : user.getFirstName() + " " + user.getLastName();
    }
}
