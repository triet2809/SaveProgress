



-- Batch 2 additive migration. Safe to run repeatedly.
-- The canonical dump remains unchanged; this only extends the audit enum.
SET search_path TO public;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = 'public' AND t.typname = 'audit_action'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = 'public' AND t.typname = 'audit_action'
          AND e.enumlabel = 'PUBLISH_RESULTS'
    ) THEN
        ALTER TYPE public.audit_action ADD VALUE 'PUBLISH_RESULTS';
    END IF;
END $$;
