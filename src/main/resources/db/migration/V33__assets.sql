CREATE TABLE assets
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(150) NOT NULL,
    category      VARCHAR(100),
    serial_number VARCHAR(150),
    status        VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    notes         TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE asset_assignments
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id        UUID NOT NULL REFERENCES assets (id) ON DELETE CASCADE,
    employee_id     UUID NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    returned_at     TIMESTAMPTZ,
    condition_notes TEXT
);

CREATE INDEX idx_asset_assignments_asset_open ON asset_assignments (asset_id) WHERE returned_at IS NULL;
CREATE INDEX idx_asset_assignments_employee_open ON asset_assignments (employee_id) WHERE returned_at IS NULL;
