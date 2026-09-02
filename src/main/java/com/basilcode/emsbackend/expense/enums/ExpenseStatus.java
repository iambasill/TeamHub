package com.basilcode.emsbackend.expense.enums;

public enum ExpenseStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Finance actually reimbursed the employee — a distinct, later step from APPROVED since
     * approval and actual payment can lag; management marks this separately. */
    PAID
}
