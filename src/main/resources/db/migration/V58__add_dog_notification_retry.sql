CREATE TABLE IF NOT EXISTS `pending_dog_notification` (
    `game_id` INT NOT NULL,
    `is_out` TINYINT(1) NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`game_id`),
    CONSTRAINT `fk_pending_dog_notification_game`
        FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`)
        ON DELETE CASCADE
);
