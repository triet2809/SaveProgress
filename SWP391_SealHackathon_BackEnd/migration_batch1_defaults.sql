-- Batch 1 data normalization for fields whose application defaults are now mapped.
-- Safe to run repeatedly against a local/development schema.
SET search_path TO public;

UPDATE public.submissions
SET review_status = 'pending'
WHERE review_status IS NULL;

UPDATE public.round_criteria
SET status = 'active'
WHERE status IS NULL;

















