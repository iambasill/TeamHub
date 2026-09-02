DO $$
BEGIN
  IF EXISTS(SELECT *
    FROM information_schema.columns
    WHERE table_name='users' and column_name='password_hash')
  THEN
      ALTER TABLE "users" RENAME COLUMN "password_hash" TO "password";
  END IF;
END $$;
