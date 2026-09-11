-- Initialize database for TrailsSpring
-- This script is run automatically by PostgreSQL on container startup

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Database is already created by POSTGRES_DB environment variable
-- Tables will be created by Flyway migrations

-- Set search path
SET search_path TO public;

-- Verify connection
SELECT 'Database initialized successfully' as status;
