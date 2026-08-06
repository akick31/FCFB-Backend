ALTER TABLE `conference_rules`
    ADD COLUMN `divisions` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`divisions`));
