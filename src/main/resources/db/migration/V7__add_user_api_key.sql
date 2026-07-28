ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `api_key_hash` VARCHAR(64) NULL;

ALTER TABLE `user`
    ADD INDEX IF NOT EXISTS `idx_user_api_key_hash` (`api_key_hash`);
