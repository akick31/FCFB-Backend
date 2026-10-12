CREATE TABLE IF NOT EXISTS `coach_season_stats` LIKE `season_stats`;

ALTER TABLE `coach_season_stats`
    DROP KEY `unique_team_season`,
    ADD COLUMN `user_id` BIGINT NOT NULL AFTER `id`,
    ADD UNIQUE KEY `unique_user_team_season` (`user_id`, `team`, `season_number`),
    ADD KEY `idx_coach_season_stats_user_id` (`user_id`);
