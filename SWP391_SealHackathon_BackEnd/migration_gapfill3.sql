-- Gap-fill migration #3: solo-team flow + short invite code + request-to-join.
-- Additive only; safe under ddl-auto=validate.

BEGIN;

-- teams: short human-friendly invite code (6 chars). Backfill existing rows.
ALTER TABLE teams ADD COLUMN IF NOT EXISTS invite_code VARCHAR(12);

-- Backfill any existing team missing a code with a random 6-char uppercase code.
UPDATE teams
SET invite_code = upper(substr(md5(random()::text || id::text), 1, 6))
WHERE invite_code IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_teams_invite_code ON teams (invite_code);

-- team_join_requests: a student requests to join a team; leader accepts/rejects.
CREATE TABLE IF NOT EXISTS team_join_requests (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id      UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    message      TEXT,
    created_at   TIMESTAMP   NOT NULL DEFAULT now(),
    responded_at TIMESTAMP,
    CONSTRAINT chk_tjr_status CHECK (status IN ('pending','accepted','rejected','cancelled'))
);
CREATE INDEX IF NOT EXISTS idx_tjr_team_id ON team_join_requests (team_id);
CREATE INDEX IF NOT EXISTS idx_tjr_user_id ON team_join_requests (user_id);
-- Only one active (pending) request per (team,user).
CREATE UNIQUE INDEX IF NOT EXISTS uq_tjr_team_user_pending
    ON team_join_requests (team_id, user_id) WHERE status = 'pending';

COMMIT;
