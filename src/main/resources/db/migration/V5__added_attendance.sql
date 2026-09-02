CREATE TABLE attendance (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            employee_id UUID NOT NULL REFERENCES employees(id),

                            check_in TIMESTAMPTZ NOT NULL,

                            check_out TIMESTAMPTZ,

                            date DATE NOT NULL,

                            status VARCHAR(20) NOT NULL DEFAULT 'PRESENT'
);