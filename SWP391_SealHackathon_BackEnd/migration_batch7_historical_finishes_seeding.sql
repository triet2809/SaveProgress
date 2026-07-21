-- Batch 7: immutable historical finishes and coordinator-controlled event seeds.
SET search_path TO public;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = 'public' AND t.typname = 'audit_action'
    ) THEN
        ALTER TYPE public.audit_action ADD VALUE IF NOT EXISTS 'FINALIZE_RESULTS';
        ALTER TYPE public.audit_action ADD VALUE IF NOT EXISTS 'ASSIGN_SEED';
        ALTER TYPE public.audit_action ADD VALUE IF NOT EXISTS 'REMOVE_SEED';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.event_team_finishes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id uuid NOT NULL REFERENCES public.events(id),
    track_id uuid NOT NULL REFERENCES public.tracks(id),
    team_id uuid NOT NULL REFERENCES public.teams(id),
    team_profile_id uuid NOT NULL REFERENCES public.team_profiles(id),
    final_round_id uuid NOT NULL REFERENCES public.rounds(id),
    result_version_id uuid NOT NULL REFERENCES public.round_result_versions(id),
    final_rank integer NOT NULL CHECK (final_rank > 0),
    final_score numeric(12,4),
    completion_status varchar(30) NOT NULL
        CHECK (completion_status IN ('completed', 'disqualified')),
    completed_at timestamp NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES public.users(id) ON DELETE SET NULL,
    row_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_event_team_finish_team UNIQUE (team_id),
    CONSTRAINT uq_event_team_finish_event_team UNIQUE (event_id, team_id)
);

CREATE INDEX IF NOT EXISTS idx_event_team_finishes_event
    ON public.event_team_finishes(event_id);
CREATE INDEX IF NOT EXISTS idx_event_team_finishes_track_rank
    ON public.event_team_finishes(track_id, final_rank);
CREATE INDEX IF NOT EXISTS idx_event_team_finishes_profile
    ON public.event_team_finishes(team_profile_id);
CREATE INDEX IF NOT EXISTS idx_event_team_finishes_version
    ON public.event_team_finishes(result_version_id);

CREATE TABLE IF NOT EXISTS public.event_seed_assignments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id uuid NOT NULL REFERENCES public.events(id),
    track_id uuid NOT NULL REFERENCES public.tracks(id),
    team_id uuid NOT NULL REFERENCES public.teams(id),
    team_profile_id uuid NOT NULL REFERENCES public.team_profiles(id),
    competition_stage varchar(40) NOT NULL DEFAULT 'event_setup',
    seed_number integer CHECK (seed_number IS NULL OR seed_number > 0),
    seed_tier varchar(20),
    candidate_source_finish_id uuid REFERENCES public.event_team_finishes(id),
    continuity_count integer NOT NULL DEFAULT 0 CHECK (continuity_count >= 0),
    status varchar(20) NOT NULL
        CHECK (status IN ('suggested', 'confirmed', 'rejected', 'overridden')),
    rationale text,
    assigned_by uuid NOT NULL REFERENCES public.users(id),
    assigned_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp,
    row_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_event_seed_team_stage UNIQUE (event_id, team_id, competition_stage),
    CONSTRAINT chk_event_seed_override_rationale
        CHECK (status <> 'overridden' OR length(trim(coalesce(rationale, ''))) > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_event_seed_number_active
    ON public.event_seed_assignments(event_id, track_id, competition_stage, seed_number)
    WHERE seed_number IS NOT NULL AND status IN ('confirmed', 'overridden');
CREATE INDEX IF NOT EXISTS idx_event_seed_assignments_event_status
    ON public.event_seed_assignments(event_id, status);
CREATE INDEX IF NOT EXISTS idx_event_seed_assignments_track
    ON public.event_seed_assignments(track_id);
CREATE INDEX IF NOT EXISTS idx_event_seed_assignments_profile
    ON public.event_seed_assignments(team_profile_id);

CREATE OR REPLACE FUNCTION public.validate_event_team_finish()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    team_track_id uuid;
    team_profile uuid;
    track_event_id uuid;
    round_track_id uuid;
    version_round_id uuid;
    version_status varchar(20);
    highest_sequence integer;
    selected_sequence integer;
BEGIN
    SELECT t.track_id, t.team_profile_id
      INTO team_track_id, team_profile
      FROM public.teams t WHERE t.id = NEW.team_id;
    SELECT tr.event_id INTO track_event_id
      FROM public.tracks tr WHERE tr.id = NEW.track_id;
    SELECT r.track_id, r.sequence_number
      INTO round_track_id, selected_sequence
      FROM public.rounds r WHERE r.id = NEW.final_round_id;
    SELECT rv.round_id, rv.status
      INTO version_round_id, version_status
      FROM public.round_result_versions rv WHERE rv.id = NEW.result_version_id;
    SELECT max(r.sequence_number) INTO highest_sequence
      FROM public.rounds r WHERE r.track_id = NEW.track_id;

    IF team_track_id IS NULL OR team_track_id <> NEW.track_id
       OR team_profile IS NULL OR team_profile <> NEW.team_profile_id
       OR track_event_id IS NULL OR track_event_id <> NEW.event_id
       OR round_track_id IS NULL OR round_track_id <> NEW.track_id
       OR version_round_id IS NULL OR version_round_id <> NEW.final_round_id THEN
        RAISE EXCEPTION 'Historical finish hierarchy mismatch' USING ERRCODE = '23514';
    END IF;
    IF version_status <> 'published' THEN
        RAISE EXCEPTION 'Historical finish requires an active published result version'
            USING ERRCODE = '23514';
    END IF;
    IF highest_sequence IS NULL OR selected_sequence <> highest_sequence THEN
        RAISE EXCEPTION 'Historical finish requires the final round for its track'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_validate_event_team_finish ON public.event_team_finishes;
CREATE TRIGGER trg_validate_event_team_finish
BEFORE INSERT ON public.event_team_finishes
FOR EACH ROW EXECUTE FUNCTION public.validate_event_team_finish();

CREATE OR REPLACE FUNCTION public.reject_event_team_finish_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Historical finish snapshots are immutable'
        USING ERRCODE = '55000';
END $$;

DROP TRIGGER IF EXISTS trg_reject_event_team_finish_mutation ON public.event_team_finishes;
CREATE TRIGGER trg_reject_event_team_finish_mutation
BEFORE UPDATE OR DELETE ON public.event_team_finishes
FOR EACH ROW EXECUTE FUNCTION public.reject_event_team_finish_mutation();

CREATE OR REPLACE FUNCTION public.validate_event_seed_assignment()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    team_track_id uuid;
    team_profile uuid;
    track_event_id uuid;
    source_profile uuid;
BEGIN
    SELECT t.track_id, t.team_profile_id
      INTO team_track_id, team_profile
      FROM public.teams t WHERE t.id = NEW.team_id;
    SELECT tr.event_id INTO track_event_id
      FROM public.tracks tr WHERE tr.id = NEW.track_id;
    IF team_track_id IS NULL OR team_track_id <> NEW.track_id
       OR team_profile IS NULL OR team_profile <> NEW.team_profile_id
       OR track_event_id IS NULL OR track_event_id <> NEW.event_id THEN
        RAISE EXCEPTION 'Seed assignment hierarchy mismatch' USING ERRCODE = '23514';
    END IF;
    IF NEW.candidate_source_finish_id IS NOT NULL THEN
        SELECT f.team_profile_id INTO source_profile
          FROM public.event_team_finishes f
         WHERE f.id = NEW.candidate_source_finish_id;
        IF source_profile IS NULL OR source_profile <> NEW.team_profile_id THEN
            RAISE EXCEPTION 'Seed source finish must belong to the same profile'
                USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_validate_event_seed_assignment ON public.event_seed_assignments;
CREATE TRIGGER trg_validate_event_seed_assignment
BEFORE INSERT OR UPDATE ON public.event_seed_assignments
FOR EACH ROW EXECUTE FUNCTION public.validate_event_seed_assignment();
