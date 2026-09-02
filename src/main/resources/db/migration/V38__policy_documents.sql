CREATE TABLE policy_documents
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    category     VARCHAR(100),
    description  TEXT,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    file_url     VARCHAR(1000),
    uploaded_by  UUID         NOT NULL REFERENCES users (id),
    uploaded_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_policy_documents_category_title ON policy_documents (category, title);
