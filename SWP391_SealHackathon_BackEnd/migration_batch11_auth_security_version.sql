-- Batch 11: invalidate credentials and security-sensitive sessions per user.
BEGIN;

SET search_path TO public;

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS security_version bigint;

UPDATE public.users
SET security_version = 1
WHERE security_version IS NULL;

ALTER TABLE public.users
    ALTER COLUMN security_version SET DEFAULT 1,
    ALTER COLUMN security_version SET NOT NULL;

COMMIT;
