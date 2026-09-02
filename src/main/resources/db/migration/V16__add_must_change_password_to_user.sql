ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT true;

-- Repair accounts created before the Lombok @Builder.Default fix: is_account_non_locked
-- was persisted as NULL, which threw a NullPointerException on login (unboxing to boolean).
UPDATE users SET is_account_non_locked = true WHERE is_account_non_locked IS NULL;
