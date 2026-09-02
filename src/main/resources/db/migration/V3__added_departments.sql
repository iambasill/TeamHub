CREATE TABLE departments (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             name        VARCHAR(100) NOT NULL UNIQUE,
                             description VARCHAR(255),
                             manager_id  UUID,
                             created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                             updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
