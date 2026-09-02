-- Migrate board_documents from BYTEA blob storage to URL-based storage.
-- Idempotent: safe to re-run if the migration was partially applied.

DO $$ BEGIN
    -- Add file_url column if it doesn't already exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'board_documents' AND column_name = 'file_url'
    ) THEN
        ALTER TABLE board_documents ADD COLUMN file_url VARCHAR(1000);
    END IF;
END $$;

DO $$ BEGIN
    -- Drop the content BYTEA column if it still exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'board_documents' AND column_name = 'content'
    ) THEN
        ALTER TABLE board_documents DROP COLUMN content;
    END IF;
END $$;
