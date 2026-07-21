-- Batch 12: logical-round uniqueness, shared metadata, and separated promotion state.
BEGIN;

SET search_path TO public;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM public.round_definitions
        GROUP BY event_id, sequence_number HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce logical-round sequence uniqueness: duplicate event_id/sequence_number rows exist';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.round_definitions
        GROUP BY event_id, lower(name) HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce logical-round name uniqueness: duplicate case-insensitive event_id/name rows exist';
    END IF;
END $$;

ALTER TABLE public.round_definitions
    ADD COLUMN IF NOT EXISTS is_final boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS default_top_n_to_promote integer,
    ADD COLUMN IF NOT EXISTS lifecycle_state varchar(40) NOT NULL DEFAULT 'SCORING';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_round_definitions_event_sequence'
          AND conrelid = 'public.round_definitions'::regclass
    ) THEN
        ALTER TABLE public.round_definitions
            ADD CONSTRAINT uq_round_definitions_event_sequence
            UNIQUE (event_id, sequence_number);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_round_definitions_event_lower_name
    ON public.round_definitions (event_id, lower(name));

CREATE TABLE IF NOT EXISTS public.logical_round_promotions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_logical_round_id uuid NOT NULL
        REFERENCES public.round_definitions(id) ON DELETE RESTRICT,
    target_logical_round_id uuid NOT NULL
        REFERENCES public.round_definitions(id) ON DELETE RESTRICT,
    team_id uuid NOT NULL REFERENCES public.teams(id) ON DELETE RESTRICT,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_logical_round_promotions_target_team
        UNIQUE (target_logical_round_id, team_id),
    CONSTRAINT ck_logical_round_promotions_distinct_rounds
        CHECK (source_logical_round_id <> target_logical_round_id)
);

CREATE INDEX IF NOT EXISTS idx_logical_round_promotions_source
    ON public.logical_round_promotions(source_logical_round_id);
CREATE INDEX IF NOT EXISTS idx_logical_round_promotions_target
    ON public.logical_round_promotions(target_logical_round_id);

CREATE OR REPLACE FUNCTION public.validate_logical_round_promotion()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    source_event uuid;
    source_sequence integer;
    target_event uuid;
    target_sequence integer;
BEGIN
    SELECT event_id, sequence_number INTO source_event, source_sequence
    FROM public.round_definitions WHERE id = NEW.source_logical_round_id;
    SELECT event_id, sequence_number INTO target_event, target_sequence
    FROM public.round_definitions WHERE id = NEW.target_logical_round_id;
    IF source_event IS NULL OR target_event IS NULL
       OR source_event <> target_event
       OR target_sequence <> source_sequence + 1 THEN
        RAISE EXCEPTION 'Promotion must target the immediately following logical round in the same event';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_validate_logical_round_promotion
    ON public.logical_round_promotions;
CREATE TRIGGER trg_validate_logical_round_promotion
BEFORE INSERT OR UPDATE ON public.logical_round_promotions
FOR EACH ROW EXECUTE FUNCTION public.validate_logical_round_promotion();

COMMIT;
