package com.basilcode.emsbackend.expense.dto;

import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.expense.entity.ExpenseClaim;
import com.basilcode.emsbackend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ExpenseClaimDtoMapper {

    public ExpenseClaimDto toDto(ExpenseClaim claim) {
        return new ExpenseClaimDto(
                claim.getId(),
                claim.getEmployee().getId(),
                fullName(claim.getEmployee()),
                claim.getCategory().name(),
                claim.getAmount(),
                claim.getDescription(),
                claim.getReceiptReference(),
                claim.getStatus().name(),
                claim.getReviewedBy() != null ? claim.getReviewedBy().getId() : null,
                fullName(claim.getReviewedBy()),
                claim.getReviewNotes(),
                claim.getCreatedAt(),
                claim.getReviewedAt(),
                claim.getPaidAt());
    }

    private String fullName(Employee employee) {
        if (employee == null || employee.getUser() == null) {
            return null;
        }
        return fullName(employee.getUser());
    }

    private String fullName(User user) {
        return user == null ? null : user.getFirstName() + " " + user.getLastName();
    }
}
