-- Real, per-user notifications, replacing the frontend's mocked notification list. Unread
-- counts are cache-aside cached in Redis (key notif:unread:{userId}); this table is the
-- source of truth the cache is rebuilt from on a miss.

CREATE TABLE notifications
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users (id),
    title        VARCHAR(255) NOT NULL,
    description  TEXT NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_id, created_at DESC);
CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_id, read);
