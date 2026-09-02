CREATE TABLE vendors
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(150) NOT NULL,
    category      VARCHAR(100),
    contact_name  VARCHAR(150),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(30),
    notes         TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
