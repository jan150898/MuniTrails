-- Cache table for Garmin activities per user
CREATE TABLE IF NOT EXISTS garmin_activity_cache (
  id          UUID PRIMARY KEY,
  user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  activities  TEXT NOT NULL,
  cached_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_garmin_activity_cache_user ON garmin_activity_cache(user_id);
