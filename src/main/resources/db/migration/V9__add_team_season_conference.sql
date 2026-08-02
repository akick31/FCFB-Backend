CREATE TABLE IF NOT EXISTS `team_season_conference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `team` VARCHAR(255) NOT NULL,
    `season_number` INT NOT NULL,
    `conference` VARCHAR(50) NULL,
    UNIQUE KEY `team_season_conference_uk` (`team`, `season_number`)
);

INSERT INTO `team_season_conference` (`team`, `season_number`, `conference`)
SELECT DISTINCT ss.team, ss.season_number, t.conference
FROM `season_stats` ss
JOIN `team` t ON t.name = ss.team
ON DUPLICATE KEY UPDATE `conference` = VALUES(`conference`);
