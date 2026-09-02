package com.basilcode.emsbackend.config;

import com.basilcode.emsbackend.board.entity.BoardMember;
import com.basilcode.emsbackend.board.repository.BoardMemberRepository;
import com.basilcode.emsbackend.department.entity.Department;
import com.basilcode.emsbackend.department.repository.DepartmentRepository;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Bootstraps a single SUPER_ADMIN account on first run — there is no self-registration
 * endpoint, so without this nobody could ever log in to a fresh database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String SEED_EMAIL = "admin@ems.com";
    private static final String SEED_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailId(SEED_EMAIL)) {
            return;
        }

        User admin = User.builder()
                .firstName("Super")
                .lastName("Admin")
                .emailId(SEED_EMAIL)
                .password(passwordEncoder.encode(SEED_PASSWORD))
                .userType(UserTypeEnum.SUPER_ADMIN)
                .mustChangePassword(false)
                .build();
        userRepository.save(admin);

        Department department = departmentRepository.findAll().stream()
                .findFirst()
                .orElse(null);

        Employee employee = new Employee();
        employee.setUser(admin);
        employee.setDepartment(department);
        employee.setJobTitle("System Administrator");
        employee.setSalary(BigDecimal.ZERO);
        employee.setHireDate(LocalDate.now());
        employee.setActive(true);
        employeeRepository.save(employee);

        // The migration that created board_members backfills existing admins at deploy time,
        // but this account doesn't exist until this runner executes (which is after migrations
        // run) — so it needs its own board membership row, re-ensured on every fresh-DB boot as
        // a break-glass safety net for the root account specifically.
        if (!boardMemberRepository.existsByUser_Id(admin.getId())) {
            BoardMember rootMembership = new BoardMember();
            rootMembership.setUser(admin);
            boardMemberRepository.save(rootMembership);
        }

        log.info("Seeded bootstrap admin account — email: {}, password: {}", SEED_EMAIL, SEED_PASSWORD);
    }
}
