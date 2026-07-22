-- Migration Batch 15: Normalize legacy FINALIZED lifecycle state
-- The enum RoundLifecycleState in code does not include FINALIZED.
-- Legacy seed scripts produced rounds with lifecycle_state='FINALIZED'.
-- This is equivalent to READY_FOR_AWARDS (terminal state for final rounds).
-- This migration is data-only, transactional, and safe to rerun.
BEGIN;

SET search_path TO public;

-- Normalize FINALIZED → READY_FOR_AWARDS in rounds table
UPDATE public.rounds
SET lifecycle_state = 'READY_FOR_AWARDS'
WHERE lifecycle_state = 'FINALIZED';

-- Normalize FINALIZED → READY_FOR_AWARDS in round_definitions table
UPDATE public.round_definitions
SET lifecycle_state = 'READY_FOR_AWARDS'
WHERE lifecycle_state = 'FINALIZED';

-- Add CHECK constraint to prevent invalid lifecycle states in future
-- This enforces at DB level that lifecycle_state must match the Java enum values
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_rounds_lifecycle_state'
        AND table_name = 'rounds'
    ) THEN
        ALTER TABLE public.rounds
        ADD CONSTRAINT chk_rounds_lifecycle_state
        CHECK (lifecycle_state IN (
            'SCORING',
            'APPEAL_WINDOW_OPEN',
            'PAUSED_FOR_APPEAL',
            'AWAITING_RECALCULATION',
            'READY_TO_ADVANCE',
            'ADVANCED',
            'READY_FOR_AWARDS'
        ));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_round_definitions_lifecycle_state'
        AND table_name = 'round_definitions'
    ) THEN
        ALTER TABLE public.round_definitions
        ADD CONSTRAINT chk_round_definitions_lifecycle_state
        CHECK (lifecycle_state IN (
            'SCORING',
            'APPEAL_WINDOW_OPEN',
            'PAUSED_FOR_APPEAL',
            'AWAITING_RECALCULATION',
            'READY_TO_ADVANCE',
            'ADVANCED',
            'READY_FOR_AWARDS'
        ));
    END IF;
END $$;

COMMIT;
