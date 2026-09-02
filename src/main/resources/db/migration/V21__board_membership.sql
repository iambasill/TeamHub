-- Board Room access is membership-based, not role-based: an ADMIN/SUPER_ADMIN user can be
-- removed from board_members and lose access despite still holding the ADMIN role, and a
-- non-admin user can be added and gain access. System admins are seeded as members here so
-- existing accounts aren't locked out on migration day; going forward, membership is managed
-- independently via the board member management endpoints.

CREATE TABLE board_members
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL UNIQUE REFERENCES users (id),
    added_by   UUID REFERENCES users (id),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE board_activity_log
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id),
    activity_type VARCHAR(20) NOT NULL,
    description   TEXT,
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_board_activity_log_user_occurred ON board_activity_log (user_id, occurred_at);

INSERT INTO board_members (id, user_id, added_by, added_at)
SELECT gen_random_uuid(), id, NULL, NOW()
FROM users
WHERE user_type IN ('ADMIN', 'SUPER_ADMIN');
