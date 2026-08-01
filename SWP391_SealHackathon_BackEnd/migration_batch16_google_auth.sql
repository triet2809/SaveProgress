-- Batch 16: Google OAuth sign-in support.
-- Adds auth_provider + google_sub columns and relaxes password_hash to allow
-- Google-only accounts (which never set a local password).
BEGIN;

SET search_path TO public;

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS google_sub varchar(255),
    ADD COLUMN IF NOT EXISTS auth_provider varchar(20);

-- Backfill existing rows as local (email + password) accounts.
UPDATE public.users
SET auth_provider = 'local'
WHERE auth_provider IS NULL;

ALTER TABLE public.users
    ALTER COLUMN auth_provider SET DEFAULT 'local',
    ALTER COLUMN auth_provider SET NOT NULL;

-- Google-only accounts have no local password.
ALTER TABLE public.users
    ALTER COLUMN password_hash DROP NOT NULL;

-- One Google identity maps to at most one account.
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_google_sub
    ON public.users (google_sub)
    WHERE google_sub IS NOT NULL;

COMMIT;
