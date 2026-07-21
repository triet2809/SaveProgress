-- Product corrections: temporary-password onboarding and auditable legal acceptance.
SET search_path TO public;

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS must_change_password boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS terms_accepted_at timestamp NULL,
    ADD COLUMN IF NOT EXISTS terms_version varchar(100) NULL,
    ADD COLUMN IF NOT EXISTS privacy_version varchar(100) NULL;
