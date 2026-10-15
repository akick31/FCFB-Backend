DROP TABLE IF EXISTS `pending_dog_notification`;

CREATE TABLE `notification_outbox` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `game_id` INT NOT NULL,
    `type` VARCHAR(32) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `payload` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    UNIQUE KEY `uq_notification_outbox_game_type` (`game_id`, `type`),
    CONSTRAINT `fk_notification_outbox_game` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`) ON DELETE CASCADE
);

CREATE TABLE `platform_transport` (
    `game_id` INT NOT NULL PRIMARY KEY,
    `home_platform` VARCHAR(255) NOT NULL,
    `home_platform_id` VARCHAR(255) NULL,
    `away_platform` VARCHAR(255) NOT NULL,
    `away_platform_id` VARCHAR(255) NULL,
    `request_message_id` LONGTEXT NULL,
    `last_message_timestamp` DATETIME NULL,
    CONSTRAINT `fk_platform_transport_game` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`) ON DELETE CASCADE
);

CREATE TABLE `game_presentation` (
    `game_id` INT NOT NULL PRIMARY KEY,
    `tv_channel` VARCHAR(255) NULL,
    `postseason_game_logo` VARCHAR(255) NULL,
    `postseason_game_name` VARCHAR(255) NULL,
    `neutral_site` TINYINT(1) NOT NULL DEFAULT 0,
    `venue` VARCHAR(255) NULL,
    CONSTRAINT `fk_game_presentation_game` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`) ON DELETE CASCADE
);

CREATE TABLE `game_record_snapshot` (
    `game_id` INT NOT NULL PRIMARY KEY,
    `home_team_rank` INT NULL,
    `away_team_rank` INT NULL,
    `home_wins` INT NULL,
    `home_losses` INT NULL,
    `away_wins` INT NULL,
    `away_losses` INT NULL,
    CONSTRAINT `fk_game_record_snapshot_game` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`) ON DELETE CASCADE
);

CREATE TABLE `game_coin_toss` (
    `game_id` INT NOT NULL PRIMARY KEY,
    `coin_toss_winner` VARCHAR(255) NULL,
    `coin_toss_choice` VARCHAR(255) NULL,
    `overtime_coin_toss_winner` VARCHAR(255) NULL,
    `overtime_coin_toss_choice` VARCHAR(255) NULL,
    CONSTRAINT `fk_game_coin_toss_game` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`) ON DELETE CASCADE
);

CREATE TABLE `game_vegas_odds` (
    `game_id` INT NOT NULL PRIMARY KEY,
    `home_vegas_spread` DOUBLE NULL,
    `away_vegas_spread` DOUBLE NULL,
    CONSTRAINT `fk_game_vegas_odds_game` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`) ON DELETE CASCADE
);

CREATE TABLE `user_credential` (
    `user_id` BIGINT NOT NULL PRIMARY KEY,
    `email` VARCHAR(255) NOT NULL,
    `hashed_email` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `salt` VARCHAR(255) NOT NULL,
    `api_key_hash` VARCHAR(255) NULL,
    `reset_token` VARCHAR(255) NULL,
    `reset_token_expiration` VARCHAR(255) NULL,
    CONSTRAINT `fk_user_credential_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

CREATE TABLE `user_preferences` (
    `user_id` BIGINT NOT NULL PRIMARY KEY,
    `delay_of_game_warning_opt_out` TINYINT(1) NOT NULL DEFAULT 0,
    `scouting_report_hidden_columns` LONGTEXT NULL,
    CONSTRAINT `fk_user_preferences_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);
