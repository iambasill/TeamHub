-- Which audience(s) an announcement targets — role-based (EMPLOYEE/HR/MANAGER/ADMIN/SUPER_ADMIN)
-- or membership-based (BOARD_MEMBERS). No row (or a row of just ALL) means everyone. An
-- announcement can target more than one audience, hence a separate table rather than a single
-- column on announcements.

CREATE TABLE announcement_audiences
(
    announcement_id UUID        NOT NULL REFERENCES announcements (id) ON DELETE CASCADE,
    audience        VARCHAR(30) NOT NULL
);

CREATE INDEX idx_announcement_audiences_announcement ON announcement_audiences (announcement_id);

-- Backfill: every existing announcement (posted before targeting existed) reached everyone.
INSERT INTO announcement_audiences (announcement_id, audience)
SELECT id, 'ALL' FROM announcements;
