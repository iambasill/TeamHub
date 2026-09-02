CREATE TABLE payroll (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         employee_id UUID NOT NULL REFERENCES employees(id),

                         pay_period DATE NOT NULL,

                         basic_salary NUMERIC(15,2) NOT NULL,

                         allowances NUMERIC(15,2) NOT NULL DEFAULT 0,

                         deductions NUMERIC(15,2) NOT NULL DEFAULT 0,

                         net_salary NUMERIC(15,2) NOT NULL,

                         status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                         processed_at TIMESTAMPTZ,

                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);