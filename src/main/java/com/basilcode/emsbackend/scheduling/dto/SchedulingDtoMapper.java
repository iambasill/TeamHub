package com.basilcode.emsbackend.scheduling.dto;

import com.basilcode.emsbackend.scheduling.entity.Shift;
import com.basilcode.emsbackend.scheduling.entity.ShiftAssignment;
import org.springframework.stereotype.Component;

@Component
public class SchedulingDtoMapper {

    public ShiftDto toDto(Shift shift) {
        return new ShiftDto(shift.getId(), shift.getName(), shift.getStartTime(), shift.getEndTime(),
                shift.getDescription(), shift.isOvernight());
    }

    public ShiftAssignmentDto toDto(ShiftAssignment assignment) {
        Shift shift = assignment.getShift();
        var employee = assignment.getEmployee();
        var user = employee.getUser();
        return new ShiftAssignmentDto(
                assignment.getId(),
                shift.getId(), shift.getName(), shift.getStartTime(), shift.getEndTime(),
                employee.getId(), user.getFirstName() + " " + user.getLastName(), employee.getJobTitle(),
                assignment.getDate());
    }
}
