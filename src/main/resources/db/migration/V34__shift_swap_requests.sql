CREATE TABLE shift_swap_requests
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_assignment_id UUID        NOT NULL REFERENCES shift_assignments (id) ON DELETE CASCADE,
    requested_by        UUID        NOT NULL REFERENCES employees (id),
    proposed_to         UUID REFERENCES employees (id),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason              TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at          TIMESTAMPTZ,
    decided_by          UUID REFERENCES users (id)
);

CREATE INDEX idx_shift_swap_requests_assignment_status ON shift_swap_requests (shift_assignment_id, status);
