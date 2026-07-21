-- Gap-fill migration #2: judge<->track assignment + mentor/judge profile fields.
-- Additive only. All new columns nullable so existing rows stay valid under ddl-auto=validate.

BEGIN;

-- users: profile fields for mentors/judges (company/expertise/bio)
ALTER TABLE users ADD COLUMN IF NOT EXISTS company   VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS expertise VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio       TEXT;

-- track_judges: judge assigned to a thematic track (mirror of track_mentors)
CREATE TABLE IF NOT EXISTS track_judges (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    track_id    UUID        NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_track_judges_track_user UNIQUE (track_id, user_id),
    CONSTRAINT uq_track_judges_event_user UNIQUE (event_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_track_judges_track_id ON track_judges (track_id);
CREATE INDEX IF NOT EXISTS idx_track_judges_user_id  ON track_judges (user_id);

COMMIT;
