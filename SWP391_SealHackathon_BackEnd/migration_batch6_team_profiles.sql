-- Batch 6: persistent team identity and cross-event registration reactivation.
-- Existing teams remain event-specific registrations. Every legacy team receives
-- its own profile; no name-based merging or historical roster rewriting occurs.
SET search_path TO public;

CREATE TABLE IF NOT EXISTS public.team_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_name varchar(255) NOT NULL,
    logo_url varchar(500),
    created_by uuid REFERENCES public.users(id) ON DELETE SET NULL,
    status varchar(20) NOT NULL DEFAULT 'active',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp,
    row_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT chk_team_profiles_status
        CHECK (status IN ('active', 'inactive', 'dissolved'))
);

ALTER TABLE public.teams
    ADD COLUMN IF NOT EXISTS team_profile_id uuid REFERENCES public.team_profiles(id);
ALTER TABLE public.teams
    ADD COLUMN IF NOT EXISTS source_team_id uuid REFERENCES public.teams(id) ON DELETE SET NULL;
ALTER TABLE public.teams
    ADD COLUMN IF NOT EXISTS activated_from_profile_at timestamp;
ALTER TABLE public.teams
    ADD COLUMN IF NOT EXISTS roster_confirmed_at timestamp;
ALTER TABLE public.teams
    ADD COLUMN IF NOT EXISTS roster_confirmed_by uuid REFERENCES public.users(id) ON DELETE SET NULL;

-- Backfill is one profile per legacy team. The historical leader is the best
-- available creator proxy because the legacy teams table has no created_by.
DO $$
DECLARE
    legacy record;
    new_profile_id uuid;
BEGIN
    FOR legacy IN
        SELECT t.id, t.name,
               (SELECT tm.user_id
                  FROM public.team_members tm
                 WHERE tm.team_id = t.id AND tm.role = 'leader'
                 ORDER BY tm.joined_at, tm.id
                 LIMIT 1) AS creator_id
          FROM public.teams t
         WHERE t.team_profile_id IS NULL
         ORDER BY t.id
    LOOP
        new_profile_id := gen_random_uuid();
        INSERT INTO public.team_profiles
            (id, canonical_name, created_by, status, created_at, updated_at, row_version)
        VALUES
            (new_profile_id, legacy.name, legacy.creator_id, 'active',
             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

        UPDATE public.teams
           SET team_profile_id = new_profile_id
         WHERE id = legacy.id AND team_profile_id IS NULL;
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.teams WHERE team_profile_id IS NULL) THEN
        ALTER TABLE public.teams ALTER COLUMN team_profile_id SET NOT NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_teams_team_profile
    ON public.teams(team_profile_id);
CREATE INDEX IF NOT EXISTS idx_teams_source_team
    ON public.teams(source_team_id);

-- Cross-table rules cannot be expressed by a plain UNIQUE/CHECK constraint
-- because an event is reached through tracks. The trigger serializes writes per
-- profile, enforces one registration per profile/event, and validates sources.
CREATE OR REPLACE FUNCTION public.validate_team_profile_registration()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    target_event_id uuid;
    source_profile_id uuid;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.team_profile_id::text, 0));

    SELECT tr.event_id INTO target_event_id
      FROM public.tracks tr
     WHERE tr.id = NEW.track_id;

    IF target_event_id IS NULL THEN
        RAISE EXCEPTION 'Team track does not exist'
            USING ERRCODE = '23503';
    END IF;

    IF NEW.source_team_id IS NOT NULL THEN
        SELECT t.team_profile_id INTO source_profile_id
          FROM public.teams t
         WHERE t.id = NEW.source_team_id;
        IF source_profile_id IS NULL OR source_profile_id <> NEW.team_profile_id THEN
            RAISE EXCEPTION 'Source team must belong to the same team profile'
                USING ERRCODE = '23514';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.teams existing
          JOIN public.tracks existing_track ON existing_track.id = existing.track_id
         WHERE existing.team_profile_id = NEW.team_profile_id
           AND existing_track.event_id = target_event_id
           AND existing.id <> NEW.id
    ) THEN
        RAISE EXCEPTION 'Team profile already has a registration in this event'
            USING ERRCODE = '23505';
    END IF;

    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_validate_team_profile_registration ON public.teams;
CREATE TRIGGER trg_validate_team_profile_registration
BEFORE INSERT OR UPDATE OF team_profile_id, track_id, source_team_id
ON public.teams
FOR EACH ROW EXECUTE FUNCTION public.validate_team_profile_registration();

