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

-- Users must be provisioned through an administrator-controlled deployment
-- process. Never ship predictable application accounts in a database migration.
