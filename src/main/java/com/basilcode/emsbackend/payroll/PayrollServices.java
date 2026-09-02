package com.basilcode.emsbackend.payroll;

import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.service.EmployeeServices;
import com.basilcode.emsbackend.payroll.dto.PayrollResponse;
import com.basilcode.emsbackend.payroll.entity.Payroll;
import com.basilcode.emsbackend.payroll.enums.PayrollStatus;
import com.basilcode.emsbackend.payroll.repository.PayrollRepository;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServices {

    private final PayrollRepository payrollRepository;
    private final EmployeeServices employeeServices;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public List<PayrollResponse> runPayroll(LocalDate payPeriod) {
        LocalDate normalizedPeriod = payPeriod.withDayOfMonth(1);
        List<Employee> activeEmployees = employeeServices.getActiveEmployeeEntities();

        List<Payroll> toGenerate = activeEmployees.stream()
                .filter(employee -> payrollRepository.findByEmployee_IdAndPayPeriod(employee.getId(), normalizedPeriod).isEmpty())
                .map(employee -> {
                    Payroll payroll = new Payroll();
                    payroll.setEmployee(employee);
                    payroll.setPayPeriod(normalizedPeriod);
                    payroll.setBasicSalary(employee.getSalary());
                    payroll.setNetSalary(employee.getSalary());
                    payroll.setStatus(PayrollStatus.PAID);
                    payroll.setProcessedAt(OffsetDateTime.now());
                    return payroll;
                })
                .toList();

        List<Payroll> saved = payrollRepository.saveAll(toGenerate);
        log.info("Payroll run for {}: generated {} payslip(s)", normalizedPeriod, saved.size());

        userRepository.findByUserTypeIn(List.of(UserTypeEnum.ADMIN, UserTypeEnum.SUPER_ADMIN)).forEach(admin ->
                notificationService.create(admin, "Payroll Disbursed",
                        normalizedPeriod + " batch processing completed — " + saved.size() + " payslip(s) generated.",
                        NotificationType.PAYROLL));

        return saved.stream().map(this::toResponse).toList();
    }

    public List<PayrollResponse> getAllPayroll() {
        return payrollRepository.findAllByOrderByPayPeriodDesc().stream().map(this::toResponse).toList();
    }

    public List<PayrollResponse> getMyPayroll(String requesterEmail) {
        Employee employee = employeeServices.getEmployeeEntityByUserEmail(requesterEmail);
        return payrollRepository.findByEmployee_IdOrderByPayPeriodDesc(employee.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public PayrollResponse getPayroll(UUID id) {
        return toResponse(payrollRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payroll record not found with id: " + id)));
    }

    public List<PayrollResponse> getPayrollForEmployee(UUID employeeId) {
        // Confirms the employee exists before returning what may be an empty payslip list.
        employeeServices.getEmployeeEntity(employeeId);
        return payrollRepository.findByEmployee_IdOrderByPayPeriodDesc(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    private PayrollResponse toResponse(Payroll payroll) {
        Employee employee = payroll.getEmployee();
        return PayrollResponse.builder()
                .id(payroll.getId())
                .employeeId(employee.getId())
                .employeeName(fullName(employee))
                .payPeriod(payroll.getPayPeriod())
                .basicSalary(payroll.getBasicSalary())
                .allowances(payroll.getAllowances())
                .deductions(payroll.getDeductions())
                .netSalary(payroll.getNetSalary())
                .status(payroll.getStatus())
                .processedAt(payroll.getProcessedAt())
                .createdAt(payroll.getCreatedAt())
                .build();
    }

    private String fullName(Employee employee) {
        User user = employee.getUser();
        return user.getFirstName() + " " + user.getLastName();
    }
}
