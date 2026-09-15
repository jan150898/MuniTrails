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
  id                              UUID PRIMARY KEY,
  track_type                      VARCHAR(32) NOT NULL,  -- Discriminator for inheritance
  type                            VARCHAR(32) NOT NULL,  -- Legacy column, canonical mapping
  status                          VARCHAR(32) NOT NULL,
  visibility                      VARCHAR(32) NOT NULL,
  name                            VARCHAR(255) NOT NULL,
  created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by_id                   UUID NOT NULL REFERENCES app_user(id),
  last_edited_by_id              UUID REFERENCES app_user(id),
  
  -- Geometry / GPX
  gpx_file                        OID,
  gpx_file_checksum              VARCHAR(64),
  bounding_box                   VARCHAR(64),
  
  -- Audit / Technical data
  distance_meters                DOUBLE PRECISION,
  elevation_gain_meters          DOUBLE PRECISION,
  elevation_loss_meters          DOUBLE PRECISION,
  highest_point_altitude_meters  DOUBLE PRECISION,
  lowest_point_altitude_meters   DOUBLE PRECISION,
  
  -- Starting point (coordinates)
  start_lat                      DOUBLE PRECISION NOT NULL,
  start_lon                      DOUBLE PRECISION NOT NULL,
  
  -- Evaluation
  overall_rating                 INTEGER,
  exposition                     INTEGER,
  uphill_rating                  INTEGER,
  ride_again                     BOOLEAN,
  
  -- Difficulty range
  difficulty_min                 VARCHAR(2),
  difficulty_max                 VARCHAR(2)
);

CREATE INDEX IF NOT EXISTS idx_gpx_track_name ON gpx_track(name);

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
