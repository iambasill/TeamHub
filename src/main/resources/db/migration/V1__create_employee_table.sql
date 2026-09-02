CREATE TABLE IF NOT EXISTS employees
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100),
    last_name  VARCHAR(100),
    email_id   VARCHAR(255) NOT NULL UNIQUE
);

CREATE EXTENSION IF NOT EXISTS pgcrypto;