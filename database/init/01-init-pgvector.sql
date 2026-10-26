-- ==============================================================================
-- Customer Feedback Intelligence Platform
-- Database Initialization Script (Phase 1)
-- ==============================================================================
-- Enables the PGVector extension for embedding vectors and UUID extension for primary keys.
-- Application tables and Flyway migrations will be introduced in Phase 2.
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Verification notice in database logs
DO $$
BEGIN
    RAISE NOTICE 'PGVector extension enabled successfully: %', (SELECT extversion FROM pg_extension WHERE extname = 'vector');
    RAISE NOTICE 'UUID extension enabled successfully: %', (SELECT extversion FROM pg_extension WHERE extname = 'uuid-ossp');
END $$;
