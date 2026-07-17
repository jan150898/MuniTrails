-- Older installations require gpx_track.type, while an early application
-- version wrote to track_type. Keep the legacy column populated and make
-- it the canonical JPA mapping.
ALTER TABLE gpx_track ADD COLUMN IF NOT EXISTS type VARCHAR(32);

UPDATE gpx_track
SET type = track_type
WHERE type IS NULL AND track_type IS NOT NULL;

ALTER TABLE gpx_track ALTER COLUMN type SET NOT NULL;
