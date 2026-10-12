ALTER TABLE team_uniform_current
    DROP COLUMN IF EXISTS jersey_text;

ALTER TABLE team_uniform_history
    DROP COLUMN IF EXISTS jersey_text;
