CREATE TABLE expense_claims
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id       UUID         NOT NULL REFERENCES employees (id),
    category          VARCHAR(20)  NOT NULL,
    amount            NUMERIC(12,2) NOT NULL,
    description       TEXT,
    receipt_reference VARCHAR(255),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by       UUID REFERENCES users (id),
    review_notes      TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    reviewed_at       TIMESTAMPTZ,
    paid_at           TIMESTAMPTZ
);

CREATE INDEX idx_expense_claims_employee_status ON expense_claims (employee_id, status);
