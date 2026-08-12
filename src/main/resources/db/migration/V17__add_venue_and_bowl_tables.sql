CREATE TABLE `venue` (
  `name` VARCHAR(255) NOT NULL,
  `city` VARCHAR(255) DEFAULT NULL,
  `state` VARCHAR(255) DEFAULT NULL,
  `capacity` INT DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `bowl` (
  `name` VARCHAR(255) NOT NULL,
  `logo` VARCHAR(500) DEFAULT NULL,
  `last_season` INT DEFAULT NULL,
  `last_home_team` VARCHAR(255) DEFAULT NULL,
  `last_away_team` VARCHAR(255) DEFAULT NULL,
  `last_home_score` INT DEFAULT NULL,
  `last_away_score` INT DEFAULT NULL,
  `last_game_id` INT DEFAULT NULL,
  `last_venue` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `bowl` (`name`, `last_venue`)
SELECT `name`, `venue` FROM `bowl_venue`;

DROP TABLE `bowl_venue`;

-- Backfill from Season 11's already-played bowls (game_type = BOWL, always week 14),
-- which have real teams/scores/venue/logo sitting in `schedule` already. Runs after the
-- bowl_venue carry-forward so real played-game data (including venue) wins over whatever
-- default venue bowl_venue happened to have on file.
INSERT INTO `bowl` (`name`, `logo`, `last_season`, `last_home_team`, `last_away_team`, `last_home_score`, `last_away_score`, `last_game_id`, `last_venue`)
SELECT `postseason_game_name`, `postseason_game_logo`, `season`, `home_team`, `away_team`, `home_score`, `away_score`, `game_id`, `venue`
FROM `schedule`
WHERE `game_type` = 'BOWL' AND `week` = 14 AND `season` = 11
  AND `postseason_game_name` IS NOT NULL AND `postseason_game_name` <> ''
ON DUPLICATE KEY UPDATE
  `logo` = VALUES(`logo`),
  `last_season` = VALUES(`last_season`),
  `last_home_team` = VALUES(`last_home_team`),
  `last_away_team` = VALUES(`last_away_team`),
  `last_home_score` = VALUES(`last_home_score`),
  `last_away_score` = VALUES(`last_away_score`),
  `last_game_id` = VALUES(`last_game_id`),
  `last_venue` = VALUES(`last_venue`);
