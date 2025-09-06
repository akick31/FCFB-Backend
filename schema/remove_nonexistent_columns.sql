-- Remove non-existent columns from league_stats table
-- These columns don't exist in the source data (SeasonStats/GameStats)

ALTER TABLE league_stats DROP COLUMN IF EXISTS extra_points_attempted;
ALTER TABLE league_stats DROP COLUMN IF EXISTS extra_points_made;
ALTER TABLE league_stats DROP COLUMN IF EXISTS extra_point_percentage;
ALTER TABLE league_stats DROP COLUMN IF EXISTS punt_yards;
ALTER TABLE league_stats DROP COLUMN IF EXISTS kickoff_returns;
ALTER TABLE league_stats DROP COLUMN IF EXISTS kickoff_return_yards;
ALTER TABLE league_stats DROP COLUMN IF EXISTS punt_returns;
ALTER TABLE league_stats DROP COLUMN IF EXISTS punt_return_yards;

-- Remove non-existent columns from playbook_stats table
-- These columns don't exist in the source data (SeasonStats/GameStats)

ALTER TABLE playbook_stats DROP COLUMN IF EXISTS extra_points_attempted;
ALTER TABLE playbook_stats DROP COLUMN IF EXISTS extra_points_made;
ALTER TABLE playbook_stats DROP COLUMN IF EXISTS extra_point_percentage;
ALTER TABLE playbook_stats DROP COLUMN IF EXISTS punt_yards;
ALTER TABLE playbook_stats DROP COLUMN IF EXISTS kickoff_returns;
ALTER TABLE playbook_stats DROP COLUMN IF EXISTS kickoff_return_yards;
ALTER TABLE playbook_stats DROP COLUMN IF EXISTS punt_returns;
ALTER TABLE playbook_stats DROP COLUMN IF EXISTS punt_return_yards;
