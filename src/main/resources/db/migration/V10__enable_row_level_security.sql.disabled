-- Enable Row Level Security (RLS) on every table in the public schema that
-- is exposed via Supabase's PostgREST API. The Spring Boot app connects
-- directly through JDBC using the table owner role, which always bypasses
-- RLS by default in Postgres - so this does NOT break the application.
-- It only prevents Supabase's REST API (anon/authenticated roles) from
-- reading or writing these tables, closing the "RLS Disabled in Public"
-- and "Sensitive Columns Exposed" findings from the Supabase linter.
--
-- No policies are added on purpose: with RLS enabled and zero policies,
-- every role except the table owner is denied access entirely, which is
-- the correct default for a database that should only be reachable
-- through this application, not through Supabase's public API.

ALTER TABLE public.flyway_schema_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.gpx_track ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tour ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trail ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.uphill ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.downhill ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.track_comment ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.spring_session ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.spring_session_attributes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.garmin_activity_cache ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.garmin_credential ENABLE ROW LEVEL SECURITY;
