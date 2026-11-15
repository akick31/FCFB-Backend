ALTER TABLE `coach_season_stats`
    ADD COLUMN `sub_games` BOOLEAN NOT NULL DEFAULT FALSE AFTER `season_number`,
    DROP KEY `unique_user_team_season`,
    ADD UNIQUE KEY `unique_user_team_season_sub_games` (`user_id`, `team`, `season_number`, `sub_games`);
