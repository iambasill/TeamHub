package com.basilcode.emsbackend.attendance;

import com.basilcode.emsbackend.attendance.dto.AttendanceReportEntryResponse;
import com.basilcode.emsbackend.attendance.dto.AttendanceResponse;
import com.basilcode.emsbackend.attendance.entity.Attendance;
import com.basilcode.emsbackend.attendance.enums.AttendanceStatus;
import com.basilcode.emsbackend.attendance.repository.AttendanceRepository;
import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.department.entity.Department;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.service.EmployeeServices;
import com.basilcode.emsbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServices {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeServices employeeServices;

    @Transactional
    public AttendanceResponse checkIn(String requesterEmail) {
        Employee employee = employeeServices.getEmployeeEntityByUserEmail(requesterEmail);
        LocalDate today = LocalDate.now();

        Optional<Attendance> existing = attendanceRepository.findByEmployee_IdAndDate(employee.getId(), today);

        // An ON_LEAVE row is just a placeholder written when the leave was approved (see
        // LeaveRequestServices) — it never represents a real check-in, so someone returning
        // early from approved leave can still check in normally instead of being blocked.
        if (existing.isPresent() && existing.get().getStatus() != AttendanceStatus.ON_LEAVE) {
            throw new BadRequestException("Already checked in today");
        }

        Attendance attendance = existing.orElseGet(Attendance::new);
        attendance.setEmployee(employee);
        attendance.setCheckIn(OffsetDateTime.now());
        attendance.setDate(today);
        attendance.setStatus(AttendanceStatus.PRESENT);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee {} checked in for {}", employee.getId(), today);
        return toResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkOut(String requesterEmail) {
        Employee employee = employeeServices.getEmployeeEntityByUserEmail(requesterEmail);
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployee_IdAndDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException("No check-in recorded for today"));

        if (attendance.getCheckOut() != null) {
            throw new BadRequestException("Already checked out today");
        }

        attendance.setCheckOut(OffsetDateTime.now());
        Attendance updated = attendanceRepository.save(attendance);
        log.info("Employee {} checked out for {}", employee.getId(), today);
        return toResponse(updated);
    }

    public List<AttendanceResponse> getMyAttendance(String requesterEmail) {
        Employee employee = employeeServices.getEmployeeEntityByUserEmail(requesterEmail);
        return attendanceRepository.findByEmployee_IdOrderByDateDesc(employee.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AttendanceResponse> getAllAttendance(LocalDate date, UUID departmentId) {
        List<Attendance> records = date != null
                ? attendanceRepository.findByDateBetweenOrderByDateDesc(date, date)
                : attendanceRepository.findAll();

        return records.stream()
                .filter(a -> departmentId == null || matchesDepartment(a, departmentId))
                .sorted(Comparator.comparing(Attendance::getDate).reversed())
                .map(this::toResponse)
                .toList();
    }

    public List<AttendanceReportEntryResponse> getMonthlyReport(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        Map<UUID, List<Attendance>> byEmployeeId = attendanceRepository.findByDateBetweenOrderByDateDesc(from, to)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        return byEmployeeId.values().stream()
                .map(records -> {
                    Employee employee = records.get(0).getEmployee();
                    return AttendanceReportEntryResponse.builder()
                            .employeeId(employee.getId())
                            .employeeName(fullName(employee))
                            .presentDays(countByStatus(records, AttendanceStatus.PRESENT))
                            .absentDays(countByStatus(records, AttendanceStatus.ABSENT))
                            .lateDays(countByStatus(records, AttendanceStatus.LATE))
                            .halfDays(countByStatus(records, AttendanceStatus.HALF_DAY))
                            .onLeaveDays(countByStatus(records, AttendanceStatus.ON_LEAVE))
                            .totalRecordedDays(records.size())
                            .build();
                })
                .sorted(Comparator.comparing(AttendanceReportEntryResponse::getEmployeeName))
                .toList();
    }

    private long countByStatus(List<Attendance> records, AttendanceStatus status) {
        return records.stream().filter(a -> a.getStatus() == status).count();
    }

    private boolean matchesDepartment(Attendance attendance, UUID departmentId) {
        Department department = attendance.getEmployee().getDepartment();
        return department != null && departmentId.equals(department.getId());
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        Employee employee = attendance.getEmployee();
        Department department = employee.getDepartment();
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .employeeId(employee.getId())
                .employeeName(fullName(employee))
                .departmentId(department != null ? department.getId() : null)
                .departmentName(department != null ? department.getName() : null)
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .build();
    }

    private String fullName(Employee employee) {
        User user = employee.getUser();
        return user.getFirstName() + " " + user.getLastName();
    }
}
