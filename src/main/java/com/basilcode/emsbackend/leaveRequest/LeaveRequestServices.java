package com.basilcode.emsbackend.leaveRequest;

import com.basilcode.emsbackend.attendance.entity.Attendance;
import com.basilcode.emsbackend.attendance.enums.AttendanceStatus;
import com.basilcode.emsbackend.attendance.repository.AttendanceRepository;
import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.exception.UnAuthorizeException;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.service.EmployeeServices;
import com.basilcode.emsbackend.leaveRequest.dto.LeaveBalanceResponse;
import com.basilcode.emsbackend.leaveRequest.dto.LeaveHistoryResponse;
import com.basilcode.emsbackend.leaveRequest.dto.LeaveRequestCreateDto;
import com.basilcode.emsbackend.leaveRequest.dto.LeaveRequestResponse;
import com.basilcode.emsbackend.leaveRequest.entity.LeaveRequest;
import com.basilcode.emsbackend.leaveRequest.repository.LeaveRequestRepository;
import com.basilcode.emsbackend.mailService.MailService;
import com.basilcode.emsbackend.notification.enums.NotificationType;
import com.basilcode.emsbackend.notification.service.NotificationService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestServices {

    // No leave-entitlement schema exists yet, so this is a flat placeholder policy
    // applied across all leave types combined.
    private static final int ANNUAL_LEAVE_ENTITLEMENT_DAYS = 20;

    private static final List<UserTypeEnum> LEAVE_APPROVER_TYPES =
            List.of(UserTypeEnum.MANAGER, UserTypeEnum.HR, UserTypeEnum.ADMIN, UserTypeEnum.SUPER_ADMIN);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeServices employeeServices;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final MailService mailService;

    @Transactional
    public LeaveRequestResponse submitLeaveRequest(String requesterEmail, LeaveRequestCreateDto request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Employee employee = employeeServices.getEmployeeEntityByUserEmail(requesterEmail);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(request.getLeaveType());
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus("PENDING");

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} submitted by employee {}", saved.getId(), employee.getId());

        String requesterName = fullName(employee);
        userRepository.findByUserTypeIn(LEAVE_APPROVER_TYPES).forEach(approver ->
                notificationService.create(approver, "New Leave Request",
                        requesterName + " requested " + request.getLeaveType() + " leave from "
                                + request.getStartDate() + " to " + request.getEndDate() + ".",
                        NotificationType.LEAVE));

        return toResponse(saved);
    }

    public LeaveHistoryResponse getMyLeaveHistory(String requesterEmail) {
        Employee employee = employeeServices.getEmployeeEntityByUserEmail(requesterEmail);
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployee_IdOrderByCreatedAtDesc(employee.getId());

        int usedDays = requests.stream()
                .filter(lr -> "APPROVED".equals(lr.getStatus()))
                .filter(lr -> lr.getStartDate().getYear() == LocalDate.now().getYear())
                .mapToInt(lr -> (int) (ChronoUnit.DAYS.between(lr.getStartDate(), lr.getEndDate()) + 1))
                .sum();

        LeaveBalanceResponse balance = LeaveBalanceResponse.builder()
                .entitlementDays(ANNUAL_LEAVE_ENTITLEMENT_DAYS)
                .usedDays(usedDays)
                .remainingDays(Math.max(0, ANNUAL_LEAVE_ENTITLEMENT_DAYS - usedDays))
                .build();

        return LeaveHistoryResponse.builder()
                .balance(balance)
                .requests(requests.stream().map(this::toResponse).toList())
                .build();
    }

    public List<LeaveRequestResponse> getAllLeaveRequests(String status, UUID employeeId) {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(lr -> status == null || status.equalsIgnoreCase(lr.getStatus()))
                .filter(lr -> employeeId == null || employeeId.equals(lr.getEmployee().getId()))
                .map(this::toResponse)
                .toList();
    }

    public LeaveRequestResponse getLeaveRequest(UUID id) {
        return toResponse(getLeaveRequestEntity(id));
    }

    @Transactional
    public LeaveRequestResponse approveLeaveRequest(UUID id, String approverEmail) {
        LeaveRequest leaveRequest = getLeaveRequestEntity(id);
        assertPending(leaveRequest);

        Employee approver = employeeServices.getEmployeeEntityByUserEmail(approverEmail);
        leaveRequest.setStatus("APPROVED");
        leaveRequest.setApprovedBy(approver);

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} approved by employee {}", id, approver.getId());

        markAttendanceOnLeave(leaveRequest);

        String message = "Your " + leaveRequest.getLeaveType() + " leave request (" + leaveRequest.getStartDate()
                + " to " + leaveRequest.getEndDate() + ") was approved by " + fullName(approver) + ".";
        notificationService.create(leaveRequest.getEmployee().getUser(), "Leave Request Approved", message, NotificationType.LEAVE);
        sendLeaveDecisionEmail(leaveRequest, "Leave Request Approved", message);

        return toResponse(updated);
    }

    /**
     * The "on leave" dashboard tile and the monthly attendance report both read from
     * {@code Attendance}, not from leave requests directly — without this, an approved leave
     * never shows up anywhere attendance is displayed. checkIn is NOT NULL on the entity, so a
     * placeholder start-of-day timestamp is used for days that have no real check-in.
     */
    private void markAttendanceOnLeave(LeaveRequest leaveRequest) {
        Employee employee = leaveRequest.getEmployee();
        LocalDate date = leaveRequest.getStartDate();
        while (!date.isAfter(leaveRequest.getEndDate())) {
            LocalDate current = date;
            Attendance attendance = attendanceRepository.findByEmployee_IdAndDate(employee.getId(), current)
                    .orElseGet(() -> {
                        Attendance a = new Attendance();
                        a.setEmployee(employee);
                        a.setDate(current);
                        a.setCheckIn(current.atStartOfDay().atOffset(ZoneOffset.UTC));
                        return a;
                    });
            attendance.setStatus(AttendanceStatus.ON_LEAVE);
            attendanceRepository.save(attendance);
            date = date.plusDays(1);
        }
    }

    /** Email is a courtesy on top of the in-app notification — a mail-server outage must never roll back the decision. */
    private void sendLeaveDecisionEmail(LeaveRequest leaveRequest, String subject, String message) {
        try {
            User recipient = leaveRequest.getEmployee().getUser();
            String body = "<div style=\"font-family: Arial, Helvetica, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;"
                    + " border: 1px solid #e5e7eb; border-radius: 8px; background: #ffffff;\">"
                    + "<h2 style=\"color: #2563eb; margin: 0 0 12px;\">" + subject + "</h2>"
                    + "<p style=\"color: #374151; font-size: 15px; line-height: 1.5; margin: 0;\">" + message + "</p></div>";
            mailService.sendMail(recipient.getEmailId(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send leave decision email for request {}: {}", leaveRequest.getId(), e.getMessage());
        }
    }

    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(UUID id, String approverEmail) {
        LeaveRequest leaveRequest = getLeaveRequestEntity(id);
        assertPending(leaveRequest);

        Employee approver = employeeServices.getEmployeeEntityByUserEmail(approverEmail);
        leaveRequest.setStatus("REJECTED");
        leaveRequest.setApprovedBy(approver);

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} rejected by employee {}", id, approver.getId());

        String message = "Your " + leaveRequest.getLeaveType() + " leave request (" + leaveRequest.getStartDate()
                + " to " + leaveRequest.getEndDate() + ") was rejected by " + fullName(approver) + ".";
        notificationService.create(leaveRequest.getEmployee().getUser(), "Leave Request Rejected", message, NotificationType.LEAVE);
        sendLeaveDecisionEmail(leaveRequest, "Leave Request Rejected", message);

        return toResponse(updated);
    }

    @Transactional
    public void cancelOwnLeaveRequest(UUID id, String requesterEmail) {
        LeaveRequest leaveRequest = getLeaveRequestEntity(id);
        Employee employee = employeeServices.getEmployeeEntityByUserEmail(requesterEmail);

        if (!leaveRequest.getEmployee().getId().equals(employee.getId())) {
            throw new UnAuthorizeException("You can only cancel your own leave requests");
        }
        assertPending(leaveRequest);

        leaveRequestRepository.delete(leaveRequest);
        log.info("Leave request {} cancelled by employee {}", id, employee.getId());
    }

    private void assertPending(LeaveRequest leaveRequest) {
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new BadRequestException("Only pending leave requests can be modified");
        }
    }

    private LeaveRequest getLeaveRequestEntity(UUID id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leave request not found with id: " + id));
    }

    private LeaveRequestResponse toResponse(LeaveRequest lr) {
        Employee approvedBy = lr.getApprovedBy();
        return LeaveRequestResponse.builder()
                .id(lr.getId())
                .employeeId(lr.getEmployee().getId())
                .employeeName(fullName(lr.getEmployee()))
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .reason(lr.getReason())
                .status(lr.getStatus())
                .approvedById(approvedBy != null ? approvedBy.getId() : null)
                .approvedByName(approvedBy != null ? fullName(approvedBy) : null)
                .createdAt(lr.getCreatedAt())
                .build();
    }

    private String fullName(Employee employee) {
        User user = employee.getUser();
        return user.getFirstName() + " " + user.getLastName();
    }
}
