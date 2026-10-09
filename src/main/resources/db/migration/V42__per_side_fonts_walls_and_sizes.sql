-- Per-side end-zone fonts, per-side walls, yard-number font, midfield logo size

ALTER TABLE team_field
    ADD COLUMN midfield_logo_size DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN left_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_design VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_text VARCHAR(255) DEFAULT NULL,
    ADD COLUMN yard_number_font VARCHAR(32) DEFAULT NULL;

ALTER TABLE game_field
    ADD COLUMN midfield_logo_size DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN left_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_design VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_text VARCHAR(255) DEFAULT NULL,
    ADD COLUMN yard_number_font VARCHAR(32) DEFAULT NULL;

ALTER TABLE bowl_field
    ADD COLUMN left_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_design VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_text VARCHAR(255) DEFAULT NULL,
    ADD COLUMN yard_number_font VARCHAR(32) DEFAULT NULL;

ALTER TABLE playoff_field
    ADD COLUMN left_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_design VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_text VARCHAR(255) DEFAULT NULL,
    ADD COLUMN yard_number_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN yard_number_source VARCHAR(16) NOT NULL DEFAULT 'FIXED';

ALTER TABLE conference_championship_field
    ADD COLUMN left_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_end_zone_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_design VARCHAR(32) DEFAULT NULL,
    ADD COLUMN right_wall_text VARCHAR(255) DEFAULT NULL,
    ADD COLUMN yard_number_font VARCHAR(32) DEFAULT NULL,
    ADD COLUMN yard_number_source VARCHAR(16) NOT NULL DEFAULT 'FIXED';

-- Per-part conference logo recolor map (JSON: source hex -> token PRIMARY/SECONDARY/TERTIARY/WHITE/BLACK)
ALTER TABLE team_field
    ADD COLUMN conference_logo_color_map TEXT DEFAULT NULL;
ALTER TABLE game_field
    ADD COLUMN conference_logo_color_map TEXT DEFAULT NULL;
