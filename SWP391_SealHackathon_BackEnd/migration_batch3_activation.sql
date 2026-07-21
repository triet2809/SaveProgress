-- Batch 3 completion: single-use hashed account activation tokens.
SET search_path TO public;

CREATE TABLE IF NOT EXISTS public.account_activation_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamp NOT NULL,
    used_at timestamp NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid NULL REFERENCES public.users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_activation_user ON public.account_activation_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_activation_expires ON public.account_activation_tokens(expires_at);
