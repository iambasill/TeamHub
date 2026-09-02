package com.basilcode.emsbackend.expense.repository;

import com.basilcode.emsbackend.expense.entity.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, UUID>,
        JpaSpecificationExecutor<ExpenseClaim> {
}
