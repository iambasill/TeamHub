CREATE TABLE resources
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    capacity    INTEGER,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE resource_bookings
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id  UUID        NOT NULL REFERENCES resources (id) ON DELETE CASCADE,
    booked_by    UUID        NOT NULL REFERENCES employees (id),
    title        VARCHAR(255) NOT NULL,
    starts_at    TIMESTAMPTZ NOT NULL,
    ends_at      TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ
);

CREATE INDEX idx_resource_bookings_resource_window ON resource_bookings (resource_id, starts_at, ends_at);
