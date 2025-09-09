-- Add team_elo column to game_stats table
ALTER TABLE game_stats 
ADD COLUMN team_elo DOUBLE DEFAULT 1500.0 COMMENT 'Team ELO rating at the time of the game';

-- Add index for better query performance
CREATE INDEX idx_game_stats_team_elo ON game_stats(team_elo);
