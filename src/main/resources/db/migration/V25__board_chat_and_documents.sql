-- Real Board Room chat and document storage, replacing the frontend's fully-mocked
-- BoardPortal. Documents are stored as bytea directly in Postgres — no existing file-storage
-- convention anywhere in this backend to follow, and this keeps the feature self-contained
-- with no filesystem/S3 dependency.

CREATE TABLE board_documents
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    content       BYTEA        NOT NULL,
    uploaded_by   UUID NOT NULL REFERENCES users (id),
    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    is_recording  BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE board_chat_messages
(
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id              UUID NOT NULL REFERENCES users (id),
    content                TEXT NOT NULL,
    sent_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    attachment_document_id UUID REFERENCES board_documents (id)
);

CREATE INDEX idx_board_chat_messages_sent_at ON board_chat_messages (sent_at);
