-- Shift definitions + the roster of employees assigned to them, for round-the-clock ops
-- coverage. Independent of the incidents module.

CREATE TABLE shifts
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    start_time  TIME         NOT NULL,
    end_time    TIME         NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE shift_assignments
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id    UUID NOT NULL REFERENCES shifts (id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    shift_date  DATE NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (shift_id, employee_id, shift_date)
);

CREATE INDEX idx_shift_assignments_date ON shift_assignments (shift_date);
