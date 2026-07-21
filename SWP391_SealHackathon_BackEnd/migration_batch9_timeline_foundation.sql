-- Batch 9: portable, user-facing competition timeline foundation.
SET search_path TO public;

ALTER TABLE public.team_timeline_events
    ALTER COLUMN team_id DROP NOT NULL;

ALTER TABLE public.team_timeline_events
    ADD COLUMN IF NOT EXISTS visibility_scope varchar(32) NOT NULL DEFAULT 'EVENT_PARTICIPANTS',
    ADD COLUMN IF NOT EXISTS event_type varchar(64),
    ADD COLUMN IF NOT EXISTS source_type varchar(64),
    ADD COLUMN IF NOT EXISTS source_id uuid,
    ADD COLUMN IF NOT EXISTS track_id uuid,
    ADD COLUMN IF NOT EXISTS submission_id uuid,
    ADD COLUMN IF NOT EXISTS appeal_id uuid,
    ADD COLUMN IF NOT EXISTS incident_id uuid,
    ADD COLUMN IF NOT EXISTS support_ticket_id uuid,
    ADD COLUMN IF NOT EXISTS seed_id uuid,
    ADD COLUMN IF NOT EXISTS recognition_id uuid,
    ADD COLUMN IF NOT EXISTS metadata jsonb,
    ADD COLUMN IF NOT EXISTS idempotency_key varchar(255);

UPDATE public.team_timeline_events
SET event_type = type
WHERE event_type IS NULL;

UPDATE public.team_timeline_events
SET visibility_scope = 'EVENT_PARTICIPANTS'
WHERE visibility_scope IS NULL OR trim(visibility_scope) = '';

ALTER TABLE public.team_timeline_events
    ALTER COLUMN event_type SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'team_timeline_events'
          AND column_name = 'metadata' AND data_type <> 'jsonb'
    ) THEN
        ALTER TABLE public.team_timeline_events
            ALTER COLUMN metadata TYPE jsonb
            USING CASE WHEN metadata IS NULL THEN NULL ELSE to_jsonb(metadata) END;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'team_timeline_events_track_id_fkey') THEN
        ALTER TABLE public.team_timeline_events
            ADD CONSTRAINT team_timeline_events_track_id_fkey
            FOREIGN KEY (track_id) REFERENCES public.tracks(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_team_timeline_events_idempotency
    ON public.team_timeline_events (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_team_timeline_events_event_time
    ON public.team_timeline_events (event_id, occurred_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_team_timeline_events_scope
    ON public.team_timeline_events (event_id, visibility_scope, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_team_timeline_events_type
    ON public.team_timeline_events (event_id, event_type, occurred_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_team_timeline_events_round_track
    ON public.team_timeline_events (round_id, track_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_team_timeline_events_event_round_time
    ON public.team_timeline_events (event_id, round_id, occurred_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_team_timeline_events_event_track_time
    ON public.team_timeline_events (event_id, track_id, occurred_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_team_timeline_events_team_time_stable
    ON public.team_timeline_events (team_id, occurred_at DESC, id DESC);
