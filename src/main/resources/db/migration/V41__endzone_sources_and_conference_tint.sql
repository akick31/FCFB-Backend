-- Bowl: per-side end-zone text + logo source
ALTER TABLE bowl_field
    ADD COLUMN left_end_zone_text VARCHAR(64) DEFAULT NULL,
    ADD COLUMN right_end_zone_text VARCHAR(64) DEFAULT NULL,
    ADD COLUMN left_end_zone_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM',
    ADD COLUMN right_end_zone_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM';

-- Playoff + conference championship: per-side end-zone logo source
ALTER TABLE playoff_field
    ADD COLUMN left_end_zone_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM',
    ADD COLUMN right_end_zone_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM';

ALTER TABLE conference_championship_field
    ADD COLUMN left_end_zone_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM',
    ADD COLUMN right_end_zone_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM';