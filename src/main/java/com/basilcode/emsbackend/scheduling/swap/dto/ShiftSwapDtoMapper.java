package com.basilcode.emsbackend.scheduling.swap.dto;

import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.scheduling.swap.entity.ShiftSwapRequest;
import com.basilcode.emsbackend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ShiftSwapDtoMapper {

    public ShiftSwapRequestDto toDto(ShiftSwapRequest request) {
        return new ShiftSwapRequestDto(
                request.getId(),
                request.getShiftAssignment().getId(),
                request.getShiftAssignment().getShift().getName(),
                request.getShiftAssignment().getDate(),
                request.getRequestedBy().getId(),
                fullName(request.getRequestedBy()),
                request.getProposedTo() != null ? request.getProposedTo().getId() : null,
                fullName(request.getProposedTo()),
                request.getStatus().name(),
                request.getReason(),
                request.getCreatedAt(),
                request.getDecidedAt(),
                fullName(request.getDecidedBy()));
    }

    private String fullName(Employee employee) {
        return employee == null ? null : fullName(employee.getUser());
    }

    private String fullName(User user) {
        return user == null ? null : user.getFirstName() + " " + user.getLastName();
    }
}
