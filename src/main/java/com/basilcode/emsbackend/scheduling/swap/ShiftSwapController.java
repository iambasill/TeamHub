package com.basilcode.emsbackend.scheduling.swap;

import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.exception.UnAuthorizeException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import com.basilcode.emsbackend.scheduling.swap.dto.ApproveShiftSwapRequest;
import com.basilcode.emsbackend.scheduling.swap.dto.CreateShiftSwapRequest;
import com.basilcode.emsbackend.scheduling.swap.dto.ShiftSwapDtoMapper;
import com.basilcode.emsbackend.scheduling.swap.dto.ShiftSwapRequestDto;
import com.basilcode.emsbackend.scheduling.swap.entity.ShiftSwapRequest;
import com.basilcode.emsbackend.scheduling.swap.enums.ShiftSwapStatus;
import com.basilcode.emsbackend.scheduling.swap.service.ShiftSwapService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Open for every employee to view and request their own swaps; approving/rejecting is allowed
 * for MANAGER/HR/ADMIN/SUPER_ADMIN or the specific coworker a request named — same "self or
 * admin" shape used for incident acknowledge/resolve, just checked inline here since it's a
 * two-way OR rather than a single role set.
 */
@RestController
@RequestMapping("/scheduling/swap-requests")
@RequiredArgsConstructor
public class ShiftSwapController {

    private static final List<String> MANAGEMENT_ROLES = List.of("ROLE_MANAGER", "ROLE_HR", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    private final ShiftSwapService shiftSwapService;
    private final ShiftSwapDtoMapper mapper;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShiftSwapRequestDto>>> list(@RequestParam(required = false) String status) {
        ShiftSwapStatus statusEnum = status == null ? null : ShiftSwapStatus.valueOf(status.toUpperCase());
        List<ShiftSwapRequestDto> requests = shiftSwapService.list(statusEnum).stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Swap requests retrieved", requests));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShiftSwapRequestDto>> create(
            @Valid @RequestBody CreateShiftSwapRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = resolveEmployee(currentUser);
        ShiftSwapRequest created = shiftSwapService.create(
                request.shiftAssignmentId(), request.proposedToEmployeeId(), request.reason(), employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Swap request created", mapper.toDto(created)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ShiftSwapRequestDto>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveShiftSwapRequest request,
            Authentication authentication,
            @AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = resolveEmployee(currentUser);
        ShiftSwapRequest existing = shiftSwapService.list(null).stream()
                .filter(r -> r.getId().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Swap request not found: " + id));
        requireManagementOrProposedTo(existing, employee, authentication);

        UUID replacementId = request != null ? request.replacementEmployeeId() : null;
        User user = resolveUser(currentUser);
        ShiftSwapRequest approved = shiftSwapService.approve(id, replacementId, user);
        return ResponseEntity.ok(ApiResponse.success("Swap request approved", mapper.toDto(approved)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ShiftSwapRequestDto>> reject(
            @PathVariable UUID id,
            Authentication authentication,
            @AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = resolveEmployee(currentUser);
        ShiftSwapRequest existing = shiftSwapService.list(null).stream()
                .filter(r -> r.getId().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Swap request not found: " + id));
        requireManagementOrProposedTo(existing, employee, authentication);

        User user = resolveUser(currentUser);
        ShiftSwapRequest rejected = shiftSwapService.reject(id, user);
        return ResponseEntity.ok(ApiResponse.success("Swap request rejected", mapper.toDto(rejected)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ShiftSwapRequestDto>> cancel(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = resolveEmployee(currentUser);
        ShiftSwapRequest cancelled = shiftSwapService.cancel(id, employee);
        return ResponseEntity.ok(ApiResponse.success("Swap request cancelled", mapper.toDto(cancelled)));
    }

    private void requireManagementOrProposedTo(ShiftSwapRequest request, Employee currentEmployee, Authentication authentication) {
        boolean isManagement = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(MANAGEMENT_ROLES::contains);
        boolean isProposedTo = request.getProposedTo() != null && request.getProposedTo().getId().equals(currentEmployee.getId());
        if (!isManagement && !isProposedTo) {
            throw new UnAuthorizeException("Only a manager/admin, or the coworker this swap was proposed to, can decide it.");
        }
    }

    private Employee resolveEmployee(UserDetails currentUser) {
        return employeeRepository.findByUser_EmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("Employee record not found for current user"));
    }

    private User resolveUser(UserDetails currentUser) {
        return userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
    }
}
