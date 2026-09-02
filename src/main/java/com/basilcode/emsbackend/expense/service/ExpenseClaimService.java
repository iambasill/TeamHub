package com.basilcode.emsbackend.expense.service;

import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.expense.entity.ExpenseClaim;
import com.basilcode.emsbackend.expense.enums.ExpenseCategory;
import com.basilcode.emsbackend.expense.enums.ExpenseStatus;
import com.basilcode.emsbackend.expense.repository.ExpenseClaimRepository;
import com.basilcode.emsbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseClaimService {

    private final ExpenseClaimRepository expenseClaimRepository;

    @Transactional
    public ExpenseClaim create(Employee employee, ExpenseCategory category, BigDecimal amount, String description, String receiptReference) {
        ExpenseClaim claim = new ExpenseClaim();
        claim.setEmployee(employee);
        claim.setCategory(category);
        claim.setAmount(amount);
        claim.setDescription(description);
        claim.setReceiptReference(receiptReference);
        return expenseClaimRepository.save(claim);
    }

    public List<ExpenseClaim> listForEmployee(UUID employeeId) {
        return search(null, employeeId);
    }

    public List<ExpenseClaim> listAll(ExpenseStatus status) {
        return search(status, null);
    }

    // A Specification (not a JPQL "?1 IS NULL OR ..." filter) so an unset filter is simply never
    // bound as a parameter — Postgres cannot infer a type for a bind parameter that only ever
    // appears in an "IS NULL" check, which raises a 42P18 "indeterminate datatype" error.
    private List<ExpenseClaim> search(ExpenseStatus status, UUID employeeId) {
        Specification<ExpenseClaim> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (employeeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        return expenseClaimRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public ExpenseClaim decide(UUID claimId, boolean approve, String reviewNotes, User reviewedBy) {
        ExpenseClaim claim = getPending(claimId);
        claim.setStatus(approve ? ExpenseStatus.APPROVED : ExpenseStatus.REJECTED);
        claim.setReviewedBy(reviewedBy);
        claim.setReviewNotes(reviewNotes);
        claim.setReviewedAt(OffsetDateTime.now());
        return expenseClaimRepository.save(claim);
    }

    @Transactional
    public ExpenseClaim markPaid(UUID claimId) {
        ExpenseClaim claim = expenseClaimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Expense claim not found: " + claimId));
        if (claim.getStatus() != ExpenseStatus.APPROVED) {
            throw new BadRequestException("Only an APPROVED claim can be marked paid (current: " + claim.getStatus() + ").");
        }
        claim.setStatus(ExpenseStatus.PAID);
        claim.setPaidAt(OffsetDateTime.now());
        return expenseClaimRepository.save(claim);
    }

    private ExpenseClaim getPending(UUID claimId) {
        ExpenseClaim claim = expenseClaimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Expense claim not found: " + claimId));
        if (claim.getStatus() != ExpenseStatus.PENDING) {
            throw new BadRequestException("This claim was already " + claim.getStatus() + ".");
        }
        return claim;
    }
}
