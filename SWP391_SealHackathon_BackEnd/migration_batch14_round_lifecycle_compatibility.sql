-- Normalize the legacy PUBLISHED lifecycle value used by older seed/import
-- scripts to the current post-publication lifecycle state.
-- This migration is data-only, transactional, and safe to rerun.
BEGIN;

SET search_path TO public;

UPDATE public.round_definitions
SET lifecycle_state = 'APPEAL_WINDOW_OPEN'
WHERE lifecycle_state = 'PUBLISHED';

UPDATE public.rounds
SET lifecycle_state = 'APPEAL_WINDOW_OPEN'
WHERE lifecycle_state = 'PUBLISHED';

COMMIT;
