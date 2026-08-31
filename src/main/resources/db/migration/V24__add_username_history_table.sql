CREATE TABLE IF NOT EXISTS `username_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `username` VARCHAR(255) NOT NULL,
    `changed_at` VARCHAR(255) NULL,
    KEY `username_history_user_id_idx` (`user_id`),
    KEY `username_history_username_idx` (`username`)
);
