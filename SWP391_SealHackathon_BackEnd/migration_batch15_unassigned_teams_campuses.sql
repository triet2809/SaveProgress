-- Restore the complete FPT campus list and allow event registrations to exist
-- before coordinator track allocation.
BEGIN;

SET search_path TO public;

DO $$
DECLARE
    fpt_university_id uuid;
BEGIN
    SELECT id INTO fpt_university_id
    FROM public.universities
    WHERE short_name = 'FPTU' OR lower(name) = 'fpt university'
    ORDER BY CASE WHEN short_name = 'FPTU' THEN 0 ELSE 1 END
    LIMIT 1;

    IF fpt_university_id IS NULL THEN
        RAISE EXCEPTION 'Batch 15 aborted: FPT University row was not found';
    END IF;

    UPDATE public.campuses
    SET name = CASE lower(city)
        WHEN 'ha noi' THEN 'FPT University Ha Noi'
        WHEN 'ho chi minh city' THEN 'FPT University Ho Chi Minh City'
        WHEN 'da nang' THEN 'FPT University Da Nang'
        WHEN 'can tho' THEN 'FPT University Can Tho'
        WHEN 'quy nhon' THEN 'FPT University Quy Nhon'
        ELSE name
    END
    WHERE university_id = fpt_university_id
      AND lower(city) IN ('ha noi', 'ho chi minh city', 'da nang', 'can tho', 'quy nhon');

    INSERT INTO public.campuses(university_id, name, city)
    SELECT fpt_university_id, campus.name, campus.city
    FROM (VALUES
        ('FPT University Ha Noi', 'Ha Noi'),
        ('FPT University Ho Chi Minh City', 'Ho Chi Minh City'),
        ('FPT University Da Nang', 'Da Nang'),
        ('FPT University Can Tho', 'Can Tho'),
        ('FPT University Quy Nhon', 'Quy Nhon')
    ) AS campus(name, city)
    WHERE NOT EXISTS (
        SELECT 1 FROM public.campuses existing
        WHERE existing.university_id = fpt_university_id
          AND (lower(existing.city) = lower(campus.city)
               OR lower(existing.name) = lower(campus.name))
    );
END $$;

ALTER TABLE public.teams ADD COLUMN IF NOT EXISTS event_id uuid;

UPDATE public.teams team
SET event_id = track.event_id
FROM public.tracks track
WHERE team.event_id IS NULL
  AND track.id = team.track_id;

DO $$
DECLARE
    unresolved_count bigint;
BEGIN
    SELECT count(*) INTO unresolved_count
    FROM public.teams
    WHERE event_id IS NULL;

    IF unresolved_count > 0 THEN
        RAISE EXCEPTION 'Batch 15 aborted: % team row(s) have no provable event', unresolved_count;
    END IF;
END $$;

ALTER TABLE public.teams ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE public.teams ALTER COLUMN track_id DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'teams_event_id_fkey') THEN
        ALTER TABLE public.teams
            ADD CONSTRAINT teams_event_id_fkey FOREIGN KEY (event_id)
            REFERENCES public.events(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_teams_event_id ON public.teams(event_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_teams_unassigned_event_name
    ON public.teams(event_id, lower(name)) WHERE track_id IS NULL;

CREATE OR REPLACE FUNCTION public.validate_team_profile_registration()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    track_event_id uuid;
    source_profile_id uuid;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.team_profile_id::text, 0));

    IF NEW.track_id IS NOT NULL THEN
        SELECT event_id INTO track_event_id FROM public.tracks WHERE id = NEW.track_id;
        IF track_event_id IS NULL THEN
            RAISE EXCEPTION 'Team track does not exist' USING ERRCODE = '23503';
        END IF;
        IF track_event_id <> NEW.event_id THEN
            RAISE EXCEPTION 'Team track must belong to the team event' USING ERRCODE = '23514';
        END IF;
    END IF;

    IF NEW.source_team_id IS NOT NULL THEN
        SELECT team_profile_id INTO source_profile_id FROM public.teams WHERE id = NEW.source_team_id;
        IF source_profile_id IS NULL OR source_profile_id <> NEW.team_profile_id THEN
            RAISE EXCEPTION 'Source team must belong to the same team profile' USING ERRCODE = '23514';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.teams existing
        WHERE existing.team_profile_id = NEW.team_profile_id
          AND existing.event_id = NEW.event_id
          AND existing.id <> NEW.id
    ) THEN
        RAISE EXCEPTION 'Team profile already has a registration in this event' USING ERRCODE = '23505';
    END IF;

    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_validate_team_profile_registration ON public.teams;
CREATE TRIGGER trg_validate_team_profile_registration
BEFORE INSERT OR UPDATE OF team_profile_id, event_id, track_id, source_team_id
ON public.teams
FOR EACH ROW EXECUTE FUNCTION public.validate_team_profile_registration();

COMMIT;
