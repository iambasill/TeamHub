package com.basilcode.emsbackend.employee.service;

import java.security.SecureRandom;
import java.util.*;

import com.basilcode.emsbackend.department.DepartmentServices;
import com.basilcode.emsbackend.department.entity.Department;
import com.basilcode.emsbackend.employee.dto.EmployeeQuerySearch;
import com.basilcode.emsbackend.mailService.MailService;
import com.basilcode.emsbackend.role.RoleServices;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.service.UserService;
import com.basilcode.emsbackend.board.storage.StorageProvider;
import com.basilcode.emsbackend.board.storage.StorageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import com.basilcode.emsbackend.common.exception.AlreadyExistsException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.exception.UnAuthorizeException;
import com.basilcode.emsbackend.employee.dto.EmployeeRequest;
import com.basilcode.emsbackend.employee.dto.EmployeeResponse;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.mapper.EmployeeMapper;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServices implements IEmployeeService {

    private static final int TEMP_PASSWORD_MIN = 100_000;
    private static final int TEMP_PASSWORD_BOUND = 900_000;

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final UserService userService;
    private final DepartmentServices departmentServices;
    private final RoleServices roleServices;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final StorageProvider storageProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * When true, POST /employees returns the generated password in its response
     * so an administrator can hand it over directly.
     *
     * Defaults to false: the password is normally delivered by email and should
     * not travel in an API response that may be logged by a proxy. Turn it on
     * when SMTP is unavailable, which is the case it exists for.
     */
    @Value("${app.employee.return-temporary-password:false}")
    private boolean returnTemporaryPassword;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        log.info("Creating employee for email: {}", request.getEmail());

        employeeRepository.findByUser_EmailId(request.getEmail()).ifPresent(e -> {
            throw new AlreadyExistsException("Employee already exists for this email");
        });

        if (userService.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("A user account already exists for this email");
        }

        Department department = departmentServices.getDepartmentEntity(request.getDepartmentId());

        String temporaryPassword = generateTemporaryPassword();

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .emailId(request.getEmail())
                .password(passwordEncoder.encode(temporaryPassword))
                .build();
        userService.createUser(user);

        Employee employee = new Employee();
        employee.setHireDate(request.getHireDate());
        employee.setJobTitle(request.getJobTitle());
        employee.setSalary(request.getSalary());
        employee.setDepartment(department);
        employee.setUser(user);
        employee.setActive(true);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            employee.setRoles(roleServices.getRoleEntities(request.getRoleIds()));
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created with id: {}", saved.getId());

        String body = mailService.renderTemplate(
                "com/basilcode/emsbackend/employee/template/employee-created-email.html",
                Map.of(
                        "firstName", request.getFirstName(),
                        "temporaryPassword", temporaryPassword
                )
        );
        mailService.sendMail(request.getEmail(), "Your EMS account has been created", body);

        EmployeeResponse response = employeeMapper.toResponse(saved);

        /*
         * sendMail is @Async and swallows its own failures, so an SMTP outage
         * never reaches this line — the employee is created either way and the
         * temporary password simply goes nowhere. It exists in plain text only
         * inside this method, and after this it is unrecoverable: the User row
         * stores a bcrypt hash.
         *
         * That is the gap this flag closes. When it is on, the caller who just
         * created the account — HR or Admin, who chose the email address a
         * moment ago — gets the password back and can pass it on by hand.
         */
        if (returnTemporaryPassword) {
            response.setTemporaryPassword(temporaryPassword);
            log.info("Returned temporary password in the create response for {}", request.getEmail());
        }

        return response;
    }

    private String generateTemporaryPassword() {
        return String.valueOf(TEMP_PASSWORD_MIN + secureRandom.nextInt(TEMP_PASSWORD_BOUND));
    }

    private static final List<UserTypeEnum> FULL_DIRECTORY_ACCESS_TYPES =
            List.of(UserTypeEnum.HR, UserTypeEnum.ADMIN, UserTypeEnum.SUPER_ADMIN);

    @Override
    public List<EmployeeResponse> getAllEmployees(EmployeeQuerySearch query, String requesterEmail) {
        Employee requester = getEmployeeEntityByUserEmail(requesterEmail);
        boolean fullAccess = hasFullDirectoryAccess(requester);

        return employeeRepository.findAll().stream()
                .filter(e -> fullAccess || sameDepartment(e, requester))
                .map(e -> toResponseScoped(e, fullAccess))
                .toList();
    }

    @Override
    public EmployeeResponse getEmployee(UUID id, String requesterEmail) {
        Employee employee = getEmployeeEntity(id);
        Employee requester = getEmployeeEntityByUserEmail(requesterEmail);
        boolean fullAccess = hasFullDirectoryAccess(requester);

        if (!fullAccess && !sameDepartment(employee, requester)) {
            throw new UnAuthorizeException("You can only view employees in your own department");
        }
        return toResponseScoped(employee, fullAccess);
    }

    /** {@code List.of(...).contains(null)} throws NPE — some legacy user rows have a null userType. */
    private boolean hasFullDirectoryAccess(Employee requester) {
        UserTypeEnum type = requester.getUser().getUserType();
        return type != null && FULL_DIRECTORY_ACCESS_TYPES.contains(type);
    }

    /** Everyone can always see their own department's roster; HR/Admin/Super Admin see everyone. */
    private boolean sameDepartment(Employee employee, Employee requester) {
        if (employee.getId().equals(requester.getId())) return true;
        Department employeeDept = employee.getDepartment();
        Department requesterDept = requester.getDepartment();
        return employeeDept != null && requesterDept != null && employeeDept.getId().equals(requesterDept.getId());
    }

    /** Salary is an HR/Admin-only field — stripped for anyone else viewing the directory. */
    private EmployeeResponse toResponseScoped(Employee employee, boolean fullAccess) {
        EmployeeResponse response = employeeMapper.toResponse(employee);
        if (!fullAccess) {
            response.setSalary(null);
        }
        return response;
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(UUID id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found with id: " + id));

        if (request.getHireDate() != null) {
            employee.setHireDate(request.getHireDate());
        }
        if (request.getJobTitle() != null) {
            employee.setJobTitle(request.getJobTitle());
        }
        if (request.getSalary() != null) {
            employee.setSalary(request.getSalary());
        }
        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentServices.getDepartmentEntity(request.getDepartmentId()));
        }
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            employee.setRoles(roleServices.getRoleEntities(request.getRoleIds()));
        }

        Employee updated = employeeRepository.save(employee);
        log.info("Employee updated with id: {}", updated.getId());
        return employeeMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found with id: " + id));
        // Soft delete — preserve data history. Also locks the linked login account: previously
        // this only flipped the HR-facing "active" flag and left isAccountNonLocked untouched, so
        // a "deactivated" employee could still log in with their existing credentials.
        employee.setActive(false);
        employee.getUser().setIsAccountNonLocked(false);
        employeeRepository.save(employee);
        log.info("Employee soft-deleted (and login locked) with id: {}", id);
    }

    @Override
    @Transactional
    public EmployeeResponse activateEmployee(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found with id: " + id));
        employee.setActive(true);
        employee.getUser().setIsAccountNonLocked(true);
        Employee activated = employeeRepository.save(employee);
        log.info("Employee reactivated (and login unlocked) with id: {}", id);
        return employeeMapper.toResponse(activated);
    }

    public EmployeeResponse getEmployeeByUserEmail(String email) {
        return employeeRepository.findByUser_EmailId(email)
                .map(employeeMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Employee profile not found for user: " + email));
    }

    @Override
    @Transactional
    public EmployeeResponse updateMyProfilePicture(String email, MultipartFile file) throws IOException {
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
        
        StorageResult result = storageProvider.store(file, "profiles");
        user.setProfilePictureUrl(result.fileUrl());
        userService.createUser(user);
        return getEmployeeByUserEmail(email);
    }

    /**
     * Returns the managed entity for use by other services (e.g. Leave/Attendance/Payroll)
     * that need to attach an Employee without reaching into EmployeeRepository directly.
     */
    public Employee getEmployeeEntity(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found with id: " + id));
    }

    public Employee getEmployeeEntityByUserEmail(String email) {
        return employeeRepository.findByUser_EmailId(email)
                .orElseThrow(() -> new NotFoundException("Employee profile not found for user: " + email));
    }

    public List<Employee> getActiveEmployeeEntities() {
        return employeeRepository.findByActiveTrue();
    }
}