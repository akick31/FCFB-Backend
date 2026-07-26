ALTER TABLE `record`
    ADD COLUMN `record_scope` VARCHAR(255) NOT NULL DEFAULT 'LEAGUE',
    ADD COLUMN `scope_value` VARCHAR(255) NULL;

CREATE INDEX `idx_record_scope_lookup`
    ON `record` (`record_name`, `record_type`, `record_scope`, `scope_value`);
