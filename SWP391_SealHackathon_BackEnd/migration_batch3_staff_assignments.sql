-- Batch 3: allow a staff member to work on multiple tracks in one event.
-- Track/user uniqueness remains enforced; this only removes the event/user
-- constraint that made multi-track assignments impossible.
SET search_path TO public;

DO $$
BEGIN
    IF to_regclass('public.track_mentors') IS NOT NULL THEN
        ALTER TABLE public.track_mentors DROP CONSTRAINT IF EXISTS uq_track_mentors_event_user;
    END IF;
    IF to_regclass('public.track_judges') IS NOT NULL THEN
        ALTER TABLE public.track_judges DROP CONSTRAINT IF EXISTS uq_track_judges_event_user;
    END IF;
END $$;
