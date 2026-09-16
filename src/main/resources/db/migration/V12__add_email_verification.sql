-- Add verification token table for email verification during registration
CREATE TABLE IF NOT EXISTS verification_token (
  id                UUID PRIMARY KEY,
  token            VARCHAR(64) NOT NULL UNIQUE,
  email            VARCHAR(255) NOT NULL UNIQUE,
  expires_at       TIMESTAMPTZ NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  verified         BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_verification_token_email ON verification_token(email);
CREATE INDEX IF NOT EXISTS idx_verification_token_expires ON verification_token(expires_at);
