CREATE TABLE leave_requests (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                employee_id  UUID    NOT NULL REFERENCES employees(id),
                                leave_type   VARCHAR(50)  NOT NULL,
                                start_date   DATE         NOT NULL,
                                end_date     DATE         NOT NULL,
                                reason       TEXT,
                                status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                                approved_by  UUID   REFERENCES employees(id),
                                created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
