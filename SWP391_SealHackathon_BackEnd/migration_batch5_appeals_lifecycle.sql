-- Appeals, immutable result snapshots, and lifecycle control.
-- Apply after the canonical dump and Batches 1-4. This file is intentionally
-- separate from the canonical dump and may be run repeatedly.
--
-- Existing rankings and appeal rows are preserved. Existing appeals retain a
-- NULL result_version_id because their historical publication contents cannot
-- be reconstructed safely. A legacy round with result_published_at but no
-- active version is adopted on its next coordinator publication: the service
-- snapshots current rankings as version 1 and opens a fresh 15-minute window.
SET search_path TO public;

CREATE TABLE IF NOT EXISTS public.round_result_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id uuid NOT NULL REFERENCES public.rounds(id) ON DELETE CASCADE,
    version_number integer NOT NULL,
    status varchar(20) NOT NULL,
    published_at timestamp NOT NULL,
    appeal_deadline timestamp NOT NULL,
    published_by uuid NULL REFERENCES public.users(id),
    source_version_id uuid NULL REFERENCES public.round_result_versions(id),
    reason text NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_result_version_number UNIQUE (round_id, version_number)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_result_version_active
    ON public.round_result_versions(round_id) WHERE status = 'published';
CREATE TABLE IF NOT EXISTS public.round_result_version_entries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    result_version_id uuid NOT NULL REFERENCES public.round_result_versions(id) ON DELETE CASCADE,
    team_id uuid NOT NULL REFERENCES public.teams(id),
    rank integer NULL,
    total_score numeric(10,2) NULL,
    promotion_status varchar(30) NOT NULL,
    tie_breaker_score numeric(10,2) NULL,
    tie_breaker_reason text NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_result_version_team UNIQUE (result_version_id, team_id),
    CONSTRAINT uq_result_version_rank UNIQUE (result_version_id, rank)
);
ALTER TABLE public.appeals ADD COLUMN IF NOT EXISTS result_version_id uuid REFERENCES public.round_result_versions(id);
ALTER TABLE public.appeals ADD COLUMN IF NOT EXISTS decision varchar(40);
ALTER TABLE public.appeals ADD COLUMN IF NOT EXISTS recalculation_required boolean NOT NULL DEFAULT false;
CREATE UNIQUE INDEX IF NOT EXISTS uq_appeal_team_version ON public.appeals(team_id, result_version_id)
    WHERE result_version_id IS NOT NULL;
ALTER TABLE public.rounds ADD COLUMN IF NOT EXISTS lifecycle_state varchar(40) NOT NULL DEFAULT 'SCORING';
ALTER TABLE public.rounds ADD COLUMN IF NOT EXISTS lifecycle_version bigint NOT NULL DEFAULT 0;
