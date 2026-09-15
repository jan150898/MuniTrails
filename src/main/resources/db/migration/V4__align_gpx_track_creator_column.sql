-- Normalize both historical creator-column layouts without assuming that the
-- obsolete created_by_user_id column exists.
ALTER TABLE gpx_track ADD COLUMN IF NOT EXISTS created_by_id UUID;

DO $$
BEGIN
	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = current_schema()
			AND table_name = 'gpx_track'
			AND column_name = 'created_by_user_id'
	) THEN
		EXECUTE 'UPDATE gpx_track
						 SET created_by_id = created_by_user_id
						 WHERE created_by_id IS NULL AND created_by_user_id IS NOT NULL';
		EXECUTE 'ALTER TABLE gpx_track ALTER COLUMN created_by_user_id DROP NOT NULL';
	END IF;
END $$;

ALTER TABLE gpx_track ALTER COLUMN created_by_id SET NOT NULL;
