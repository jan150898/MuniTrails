-- Older databases require created_by_id, while the initial migration created
-- created_by_user_id.  Copy the authenticated user's ID to the canonical
-- legacy column and allow either layout during the transition.
ALTER TABLE gpx_track ADD COLUMN IF NOT EXISTS created_by_id UUID;

UPDATE gpx_track
SET created_by_id = created_by_user_id
WHERE created_by_id IS NULL AND created_by_user_id IS NOT NULL;

ALTER TABLE gpx_track ALTER COLUMN created_by_id SET NOT NULL;
ALTER TABLE gpx_track ALTER COLUMN created_by_user_id DROP NOT NULL;
