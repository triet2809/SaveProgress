-- Product corrections: event-level logical round identity with per-track executions.
BEGIN;

SET search_path TO public;

CREATE TABLE IF NOT EXISTS public.round_definitions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id uuid NOT NULL REFERENCES public.events(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    sequence_number integer NOT NULL CHECK (sequence_number > 0),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_round_definitions_event_name_sequence
    ON public.round_definitions(event_id, lower(name), sequence_number);
CREATE INDEX IF NOT EXISTS idx_round_definitions_event_sequence
    ON public.round_definitions(event_id, sequence_number);

ALTER TABLE public.rounds
    ADD COLUMN IF NOT EXISTS logical_round_id uuid NULL;

INSERT INTO public.round_definitions(id, event_id, name, sequence_number)
SELECT gen_random_uuid(), source.event_id, source.name, source.sequence_number
FROM (
    SELECT tr.event_id, min(r.name) AS name, r.sequence_number, lower(r.name) AS normalized_name
    FROM public.rounds r
    JOIN public.tracks tr ON tr.id = r.track_id
    GROUP BY tr.event_id, r.sequence_number, lower(r.name)
) source
WHERE NOT EXISTS (
    SELECT 1
    FROM public.round_definitions d
    WHERE d.event_id = source.event_id
      AND d.sequence_number = source.sequence_number
      AND lower(d.name) = source.normalized_name
);

UPDATE public.rounds r
SET logical_round_id = d.id
FROM public.tracks tr, public.round_definitions d
WHERE r.logical_round_id IS NULL
  AND tr.id = r.track_id
  AND d.event_id = tr.event_id
  AND d.sequence_number = r.sequence_number
  AND lower(d.name) = lower(r.name);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_rounds_logical_round'
          AND conrelid = 'public.rounds'::regclass
    ) THEN
        ALTER TABLE public.rounds
            ADD CONSTRAINT fk_rounds_logical_round
            FOREIGN KEY (logical_round_id) REFERENCES public.round_definitions(id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_rounds_logical_track'
          AND conrelid = 'public.rounds'::regclass
    ) THEN
        ALTER TABLE public.rounds
            ADD CONSTRAINT uq_rounds_logical_track UNIQUE (logical_round_id, track_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.rounds WHERE logical_round_id IS NULL) THEN
        ALTER TABLE public.rounds ALTER COLUMN logical_round_id SET NOT NULL;
    END IF;
END $$;

COMMIT;
