-- Email addresses are private login identifiers. Usernames remain the names
-- displayed on comments and uploads. Existing accounts must be assigned an
-- email address by an administrator before they can sign in.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email VARCHAR(320);

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_email_lower
  ON app_user (LOWER(email))
  WHERE email IS NOT NULL;
