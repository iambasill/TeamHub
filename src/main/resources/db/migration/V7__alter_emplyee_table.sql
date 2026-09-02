ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS phone           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS gender          VARCHAR(10),
    ADD COLUMN IF NOT EXISTS date_of_birth   DATE,
    ADD COLUMN IF NOT EXISTS hire_date       DATE          NOT NULL DEFAULT CURRENT_DATE,
    ADD COLUMN IF NOT EXISTS job_title       VARCHAR(150),
    ADD COLUMN IF NOT EXISTS salary          NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS department_id   UUID REFERENCES departments(id),
    ADD COLUMN IF NOT EXISTS is_active       BOOLEAN       NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW();

-- unique constraint on email_id — add only if not already present


CREATE TABLE IF NOT EXISTS employee_roles (
                                              employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                                              role_id     UUID NOT NULL REFERENCES roles(id),
                                              PRIMARY KEY (employee_id, role_id)
);