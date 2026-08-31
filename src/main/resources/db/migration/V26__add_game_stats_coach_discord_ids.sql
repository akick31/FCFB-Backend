ALTER TABLE `game_stats` ADD COLUMN `coach_discord_ids` LONGTEXT NULL;

UPDATE `game_stats` gs
JOIN `game` g ON gs.game_id = g.game_id
SET gs.coach_discord_ids = CASE WHEN gs.team = g.home_team THEN g.home_coach_discord_ids ELSE g.away_coach_discord_ids END
WHERE gs.team = g.home_team OR gs.team = g.away_team;
