-- Batch 8: persistent, auditable team-profile recognitions.
SET search_path TO public;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM pg_type t
          JOIN pg_namespace n ON n.oid = t.typnamespace
         WHERE n.nspname = 'public'
           AND t.typname = 'audit_action'
    ) THEN
        ALTER TYPE public.audit_action
            ADD VALUE IF NOT EXISTS 'TEAM_RECOGNITION_AWARDED';
        ALTER TYPE public.audit_action
            ADD VALUE IF NOT EXISTS 'TEAM_RECOGNITION_REVOKED';
        ALTER TYPE public.audit_action
            ADD VALUE IF NOT EXISTS 'TEAM_RECOGNITION_RESTORED';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.team_recognitions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_profile_id uuid NOT NULL REFERENCES public.team_profiles(id),
    recognition_code varchar(80) NOT NULL,
    label varchar(160) NOT NULL,
    qualification_count integer NOT NULL DEFAULT 0
        CHECK (qualification_count >= 0),
    earned_at timestamp NOT NULL,
    active boolean NOT NULL DEFAULT true,
    revoked_at timestamp,
    revoked_by uuid REFERENCES public.users(id) ON DELETE SET NULL,
    revoke_reason text,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp,
    row_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT chk_team_recognition_revocation
        CHECK (
            (active = true AND revoked_at IS NULL AND revoked_by IS NULL AND revoke_reason IS NULL)
            OR
            (active = false AND revoked_at IS NOT NULL
                AND length(trim(coalesce(revoke_reason, ''))) > 0)
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_team_recognition_active
    ON public.team_recognitions(team_profile_id, recognition_code)
    WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_team_recognitions_profile
    ON public.team_recognitions(team_profile_id);

CREATE INDEX IF NOT EXISTS idx_team_recognitions_code_active
    ON public.team_recognitions(recognition_code, active);

-- Deliberately no SQL backfill: existing profiles are evaluated only by the
-- coordinator-reviewed application recalculation workflow.
