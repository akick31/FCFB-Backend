ALTER TABLE `records`
    ADD COLUMN IF NOT EXISTS `record_scope` VARCHAR(50) NOT NULL DEFAULT 'LEAGUE',
    ADD COLUMN IF NOT EXISTS `scope_value` VARCHAR(100) NULL;

ALTER TABLE `records`
    ADD INDEX IF NOT EXISTS `idx_record_scope_lookup` (`record_name`(40), `record_type`(40), `record_scope`, `scope_value`(60));
