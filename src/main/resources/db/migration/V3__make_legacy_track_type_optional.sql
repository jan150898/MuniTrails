-- `track_type` was introduced alongside the legacy `type` column.  The
-- application now writes `type`; preserve old values but do not require both
-- columns for every new track.
ALTER TABLE gpx_track ALTER COLUMN track_type DROP NOT NULL;

UPDATE gpx_track
SET track_type = type
WHERE track_type IS NULL AND type IS NOT NULL;
