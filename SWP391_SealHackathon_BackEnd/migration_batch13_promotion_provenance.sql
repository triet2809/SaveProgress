-- Batch 13: immutable published-result provenance for logical promotions.
BEGIN;

SET search_path TO public;

ALTER TABLE public.logical_round_promotions
    ADD COLUMN IF NOT EXISTS source_result_version_id uuid,
    ADD COLUMN IF NOT EXISTS source_result_entry_id uuid;

-- Backfill only an unambiguous active-published promoted entry from the source
-- logical round. Never guess when multiple executions/results could qualify.
WITH candidates AS (
    SELECT p.id AS promotion_id,
           v.id AS version_id,
           e.id AS entry_id,
           COUNT(*) OVER (PARTITION BY p.id) AS candidate_count
    FROM public.logical_round_promotions p
    JOIN public.rounds source_execution
      ON source_execution.logical_round_id = p.source_logical_round_id
    JOIN public.round_result_versions v
      ON v.round_id = source_execution.id
     AND v.status = 'published'
    JOIN public.round_result_version_entries e
      ON e.result_version_id = v.id
     AND e.team_id = p.team_id
     AND e.promotion_status = 'promoted'
    WHERE p.source_result_version_id IS NULL
      AND p.source_result_entry_id IS NULL
), unique_candidates AS (
    SELECT DISTINCT ON (promotion_id) promotion_id, version_id, entry_id
    FROM candidates
    WHERE candidate_count = 1
    ORDER BY promotion_id
)
UPDATE public.logical_round_promotions p
SET source_result_version_id = c.version_id,
    source_result_entry_id = c.entry_id
FROM unique_candidates c
WHERE p.id = c.promotion_id;

DO $$
DECLARE
    unresolved bigint;
BEGIN
    SELECT COUNT(*) INTO unresolved
    FROM public.logical_round_promotions
    WHERE source_result_version_id IS NULL
       OR source_result_entry_id IS NULL;
    IF unresolved > 0 THEN
        RAISE EXCEPTION
            'Batch 13 aborted: % logical promotion row(s) have unresolved published-result provenance; resolve them explicitly before retrying',
            unresolved;
    END IF;
END $$;

ALTER TABLE public.logical_round_promotions
    ALTER COLUMN source_result_version_id SET NOT NULL,
    ALTER COLUMN source_result_entry_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_result_entry_id_version'
          AND conrelid = 'public.round_result_version_entries'::regclass
    ) THEN
        ALTER TABLE public.round_result_version_entries
            ADD CONSTRAINT uq_result_entry_id_version UNIQUE (id, result_version_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_promotion_source_result_version'
          AND conrelid = 'public.logical_round_promotions'::regclass
    ) THEN
        ALTER TABLE public.logical_round_promotions
            ADD CONSTRAINT fk_promotion_source_result_version
            FOREIGN KEY (source_result_version_id)
            REFERENCES public.round_result_versions(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_promotion_source_result_entry'
          AND conrelid = 'public.logical_round_promotions'::regclass
    ) THEN
        ALTER TABLE public.logical_round_promotions
            ADD CONSTRAINT fk_promotion_source_result_entry
            FOREIGN KEY (source_result_entry_id)
            REFERENCES public.round_result_version_entries(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_promotion_entry_version'
          AND conrelid = 'public.logical_round_promotions'::regclass
    ) THEN
        ALTER TABLE public.logical_round_promotions
            ADD CONSTRAINT fk_promotion_entry_version
            FOREIGN KEY (source_result_entry_id, source_result_version_id)
            REFERENCES public.round_result_version_entries(id, result_version_id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE OR REPLACE FUNCTION public.validate_logical_round_promotion_provenance()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    source_event uuid;
    source_sequence integer;
    target_event uuid;
    target_sequence integer;
    version_round uuid;
    version_status varchar;
    active_published uuid;
    entry_version uuid;
    entry_team uuid;
    entry_status varchar;
    active_count integer;
BEGIN
    SELECT event_id, sequence_number INTO source_event, source_sequence
    FROM public.round_definitions WHERE id = NEW.source_logical_round_id;
    SELECT event_id, sequence_number INTO target_event, target_sequence
    FROM public.round_definitions WHERE id = NEW.target_logical_round_id;
    SELECT r.logical_round_id, v.status
      INTO version_round, version_status
    FROM public.round_result_versions v
    JOIN public.rounds r ON r.id = v.round_id
    WHERE v.id = NEW.source_result_version_id;
    SELECT COUNT(*) INTO active_count
    FROM public.round_result_versions v
    WHERE v.round_id = (SELECT round_id FROM public.round_result_versions WHERE id = NEW.source_result_version_id)
      AND v.status = 'published';
    IF active_count = 1 THEN
        SELECT v.id INTO active_published
        FROM public.round_result_versions v
        WHERE v.round_id = (SELECT round_id FROM public.round_result_versions WHERE id = NEW.source_result_version_id)
          AND v.status = 'published';
    END IF;
    SELECT result_version_id, team_id, promotion_status
      INTO entry_version, entry_team, entry_status
    FROM public.round_result_version_entries
    WHERE id = NEW.source_result_entry_id;

    IF source_event IS NULL OR target_event IS NULL
       OR source_event <> target_event
       OR target_sequence <> source_sequence + 1
       OR version_round IS NULL
       OR version_round <> NEW.source_logical_round_id
       OR version_status <> 'published'
       OR active_count <> 1
       OR active_published <> NEW.source_result_version_id
       OR entry_version <> NEW.source_result_version_id
       OR entry_team <> NEW.team_id
       OR entry_status <> 'promoted' THEN
        RAISE EXCEPTION
            'Promotion provenance must reference the active published promoted entry from the immediately preceding logical round';
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION public.reject_promotion_provenance_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.source_result_version_id <> OLD.source_result_version_id
       OR NEW.source_result_entry_id <> OLD.source_result_entry_id THEN
        RAISE EXCEPTION 'Logical promotion result provenance is immutable';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_validate_logical_round_promotion_provenance
    ON public.logical_round_promotions;
CREATE TRIGGER trg_validate_logical_round_promotion_provenance
BEFORE INSERT OR UPDATE ON public.logical_round_promotions
FOR EACH ROW EXECUTE FUNCTION public.validate_logical_round_promotion_provenance();

DROP TRIGGER IF EXISTS trg_reject_promotion_provenance_mutation
    ON public.logical_round_promotions;
CREATE TRIGGER trg_reject_promotion_provenance_mutation
BEFORE UPDATE ON public.logical_round_promotions
FOR EACH ROW EXECUTE FUNCTION public.reject_promotion_provenance_mutation();

CREATE INDEX IF NOT EXISTS idx_logical_round_promotions_source_version
    ON public.logical_round_promotions(source_result_version_id);
CREATE INDEX IF NOT EXISTS idx_logical_round_promotions_source_entry
    ON public.logical_round_promotions(source_result_entry_id);

COMMIT;
