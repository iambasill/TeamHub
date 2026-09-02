CREATE TABLE roles (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name        VARCHAR(50)  NOT NULL UNIQUE,
                       description VARCHAR(255),
                       created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


INSERT INTO roles (name) VALUES
                             ('ROLE_ADMIN'), ('ROLE_HR'), ('ROLE_MANAGER'), ('ROLE_EMPLOYEE');
