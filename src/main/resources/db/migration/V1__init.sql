-- Initial schema for TrailsSpring (Flyway)
-- Uses PostgreSQL.

-- User
CREATE TABLE IF NOT EXISTS app_user (
  id              UUID PRIMARY KEY,
  username        VARCHAR(255) NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL,
  role            VARCHAR(64)  NOT NULL,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- GPX Track base + joined inheritance subclasses
CREATE TABLE IF NOT EXISTS gpx_track (
  id                  UUID PRIMARY KEY,
  track_type          VARCHAR(32) NOT NULL,
  status              VARCHAR(32) NOT NULL,
  visibility          VARCHAR(32) NOT NULL,
  name                VARCHAR(255) NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by_user_id UUID NOT NULL REFERENCES app_user(id),
  last_edited_by_id  UUID REFERENCES app_user(id),

  -- Minimal fields for now (extend later)
  gpx_file_name       VARCHAR(512),
  bounding_box        TEXT,
  length_meters       DOUBLE PRECISION,
  number_of_track_points INTEGER,
  start_latitude      DOUBLE PRECISION,
  start_longitude     DOUBLE PRECISION,
  end_latitude        DOUBLE PRECISION,
  end_longitude       DOUBLE PRECISION,
  start_elevation_m   DOUBLE PRECISION,
  end_elevation_m     DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS tour (
  id UUID PRIMARY KEY REFERENCES gpx_track(id)
);

CREATE TABLE IF NOT EXISTS trail (
  id UUID PRIMARY KEY REFERENCES gpx_track(id)
);

CREATE TABLE IF NOT EXISTS uphill (
  id UUID PRIMARY KEY REFERENCES gpx_track(id)
);

CREATE TABLE IF NOT EXISTS downhill (
  id UUID PRIMARY KEY REFERENCES gpx_track(id)
);

-- Comments
CREATE TABLE IF NOT EXISTS track_comment (
  id           UUID PRIMARY KEY,
  track_id     UUID NOT NULL REFERENCES gpx_track(id) ON DELETE CASCADE,
  user_id      UUID NOT NULL REFERENCES app_user(id),
  text         TEXT NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- Photos (placeholder)
  photos_json  TEXT
);

CREATE INDEX IF NOT EXISTS idx_track_comment_track ON track_comment(track_id);
CREATE INDEX IF NOT EXISTS idx_track_comment_user  ON track_comment(user_id);

-- Seed initial users (passwords must match your BCrypt encoder output)
-- IMPORTANT: Replace these hashes if your DbUserDetailsService expects different columns/names.
-- To keep migrations deterministic, we insert with empty placeholders only if table is empty.
-- If you want real accounts immediately, set passwords via application/service layer instead.
INSERT INTO app_user (id, username, password_hash, role)
SELECT
  gen_random_uuid(),
  'admin',
  '$2b$10$ZBqfIqFdG669xLHi4yE9/uyMgLtfV3WTBVryt5.vNQ01f8Nzwzd5q',
  'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username='admin');

INSERT INTO app_user (id, username, password_hash, role)
SELECT
  gen_random_uuid(),
  'user',
  '$2b$10$hnhb5dcNsm6IQXcvlt0YAOQ2GlIugprRuzg66lxZkwCqiBcrBOsDW',
  'USER'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username='user');
