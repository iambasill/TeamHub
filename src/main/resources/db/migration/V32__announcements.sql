CREATE TABLE announcements
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    body        TEXT         NOT NULL,
    pinned      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by  UUID         NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ
);

CREATE INDEX idx_announcements_pinned_created ON announcements (pinned DESC, created_at DESC);
