-- Gap-fill migration: add columns for FE fields that had no BE equivalent.
-- All nullable so existing rows stay valid under ddl-auto=validate.
-- Counts (rounds/tracks/participants) are DERIVED in service, not stored.

BEGIN;

-- events: term/prize + registration & event date windows
ALTER TABLE events ADD COLUMN IF NOT EXISTS term               VARCHAR(255);
ALTER TABLE events ADD COLUMN IF NOT EXISTS prize_pool         VARCHAR(255);
ALTER TABLE events ADD COLUMN IF NOT EXISTS registration_start TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS registration_end   TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_start        TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_end          TIMESTAMP;

-- submissions: project name/version/review status
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS project_name  VARCHAR(255);
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS version       VARCHAR(50);
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS review_status VARCHAR(50) DEFAULT 'pending';

-- incident_reports: severity/category
ALTER TABLE incident_reports ADD COLUMN IF NOT EXISTS severity VARCHAR(50);
ALTER TABLE incident_reports ADD COLUMN IF NOT EXISTS category VARCHAR(100);

-- users: contact/profile
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone       VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS department  VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS position    VARCHAR(255);

-- round_criteria: status
ALTER TABLE round_criteria ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'active';

-- notices: target team
ALTER TABLE notices ADD COLUMN IF NOT EXISTS target_team_id UUID;

COMMIT;
