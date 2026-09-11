package com.basilcode.emsbackend.expense;

import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import com.basilcode.emsbackend.expense.dto.CreateExpenseClaimRequest;
import com.basilcode.emsbackend.expense.dto.ExpenseClaimDto;
import com.basilcode.emsbackend.expense.dto.ExpenseClaimDtoMapper;
import com.basilcode.emsbackend.expense.dto.ReviewExpenseClaimRequest;
import com.basilcode.emsbackend.expense.entity.ExpenseClaim;
import com.basilcode.emsbackend.expense.enums.ExpenseCategory;
import com.basilcode.emsbackend.expense.enums.ExpenseStatus;
import com.basilcode.emsbackend.expense.service.ExpenseClaimService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** {@code GET /expenses} is always "my claims"; {@code GET /expenses/all} is the management view —
 * a deliberate split rather than one endpoint with an implicit role branch, since expense amounts
 * are more sensitive than e.g. incidents and shouldn't default to visible to everyone. */
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseClaimController {

    private final ExpenseClaimService expenseClaimService;
    private final ExpenseClaimDtoMapper mapper;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseClaimDto>>> listMine(@AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = resolveEmployee(currentUser);
        List<ExpenseClaimDto> claims = expenseClaimService.listForEmployee(employee.getId()).stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Your expense claims retrieved", claims));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ExpenseClaimDto>>> listAll(@RequestParam(required = false) String status) {
        ExpenseStatus statusEnum = status == null ? null : parseStatus(status);
        List<ExpenseClaimDto> claims = expenseClaimService.listAll(statusEnum).stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("All expense claims retrieved", claims));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseClaimDto>> create(
            @Valid @RequestBody CreateExpenseClaimRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = resolveEmployee(currentUser);
        ExpenseCategory category = parseCategory(request.category());
        ExpenseClaim claim = expenseClaimService.create(employee, category, request.amount(), request.description(), request.receiptReference());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Expense claim submitted", mapper.toDto(claim)));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ExpenseClaimDto>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewExpenseClaimRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        String notes = request != null ? request.reviewNotes() : null;
        ExpenseClaim claim = expenseClaimService.decide(id, true, notes, user);
        return ResponseEntity.ok(ApiResponse.success("Expense claim approved", mapper.toDto(claim)));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ExpenseClaimDto>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewExpenseClaimRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        String notes = request != null ? request.reviewNotes() : null;
        ExpenseClaim claim = expenseClaimService.decide(id, false, notes, user);
        return ResponseEntity.ok(ApiResponse.success("Expense claim rejected", mapper.toDto(claim)));
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<ApiResponse<ExpenseClaimDto>> markPaid(@PathVariable UUID id) {
        ExpenseClaim claim = expenseClaimService.markPaid(id);
        return ResponseEntity.ok(ApiResponse.success("Expense claim marked paid", mapper.toDto(claim)));
    }

    private ExpenseCategory parseCategory(String value) {
        try {
            return ExpenseCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category: " + value);
        }
    }

    private ExpenseStatus parseStatus(String value) {
        try {
            return ExpenseStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + value);
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
