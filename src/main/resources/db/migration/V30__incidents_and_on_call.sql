-- Company-wide operational incident tracking + on-call roster. Every employee can see and raise
-- an incident.

CREATE TABLE incidents
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    severity          VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    source            VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    source_ref        VARCHAR(255),
    assigned_to       UUID REFERENCES users (id),
    created_by        UUID REFERENCES users (id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    acknowledged_at   TIMESTAMPTZ,
    resolved_at       TIMESTAMPTZ,
    resolution_notes  TEXT
);

CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_source_ref ON incidents (source_ref);

CREATE TABLE on_call_assignments
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ NOT NULL,
    created_by  UUID NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_on_call_window ON on_call_assignments (starts_at, ends_at);
