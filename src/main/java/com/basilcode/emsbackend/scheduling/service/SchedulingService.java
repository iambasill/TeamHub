package com.basilcode.emsbackend.scheduling.service;

import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import com.basilcode.emsbackend.scheduling.entity.Shift;
import com.basilcode.emsbackend.scheduling.entity.ShiftAssignment;
import com.basilcode.emsbackend.scheduling.repository.ShiftAssignmentRepository;
import com.basilcode.emsbackend.scheduling.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shift definitions + the roster built from assigning employees to them. "Currently on duty" has
 * to look at both today's and yesterday's assignments because an overnight shift (e.g. 22:00-06:00)
 * is recorded under the date it starts, so the early-morning hours of "now" can belong to a shift
 * dated yesterday.
 */
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public Shift createShift(String name, LocalTime startTime, LocalTime endTime, String description) {
        if (startTime.equals(endTime)) {
            throw new BadRequestException("startTime and endTime cannot be identical — a zero-length shift isn't meaningful.");
        }
        Shift shift = new Shift();
        shift.setName(name);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setDescription(description);
        return shiftRepository.save(shift);
    }

    public List<Shift> listShifts() {
        return shiftRepository.findAllByOrderByStartTime();
    }

    public List<ShiftAssignment> listRoster(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BadRequestException("end date cannot be before start date");
        }
        return shiftAssignmentRepository.findByDateBetweenOrderByDateAscShift_StartTimeAsc(start, end);
    }

    @Transactional
    public ShiftAssignment assign(UUID shiftId, UUID employeeId, LocalDate date) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("Shift not found: " + shiftId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found: " + employeeId));
        if (shiftAssignmentRepository.existsByShift_IdAndEmployee_IdAndDate(shiftId, employeeId, date)) {
            throw new BadRequestException("This employee is already rostered onto this shift on " + date + ".");
        }

        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setShift(shift);
        assignment.setEmployee(employee);
        assignment.setDate(date);
        return shiftAssignmentRepository.save(assignment);
    }

    @Transactional
    public void removeAssignment(UUID id) {
        if (!shiftAssignmentRepository.existsById(id)) {
            throw new NotFoundException("Shift assignment not found: " + id);
        }
        shiftAssignmentRepository.deleteById(id);
    }

    public List<ShiftAssignment> currentlyOnDuty() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalTime now = LocalTime.now();

        List<ShiftAssignment> candidates = new ArrayList<>();
        candidates.addAll(shiftAssignmentRepository.findByDateOrderByShift_StartTimeAsc(today));
        candidates.addAll(shiftAssignmentRepository.findByDateOrderByShift_StartTimeAsc(yesterday));

        return candidates.stream().filter(a -> isActiveNow(a, today, now)).toList();
    }

    private boolean isActiveNow(ShiftAssignment assignment, LocalDate today, LocalTime now) {
        Shift shift = assignment.getShift();
        if (!shift.isOvernight()) {
            return assignment.getDate().equals(today)
                    && !now.isBefore(shift.getStartTime())
                    && !now.isAfter(shift.getEndTime());
        }
        if (assignment.getDate().equals(today)) {
            // Late-night half: from the shift's start until midnight, on its own start date.
            return !now.isBefore(shift.getStartTime());
        }
        // Early-morning half: assignment dated yesterday, running from midnight to the shift's end.
        return !now.isAfter(shift.getEndTime());
    }
}
