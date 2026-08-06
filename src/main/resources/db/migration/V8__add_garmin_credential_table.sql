-- Encrypted Garmin credentials per user
CREATE TABLE IF NOT EXISTS garmin_credential (
  id                 UUID PRIMARY KEY,
  user_id            UUID NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
  encrypted_email    VARCHAR(500) NOT NULL,
  encrypted_password VARCHAR(500) NOT NULL,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_garmin_credential_user ON garmin_credential(user_id);
