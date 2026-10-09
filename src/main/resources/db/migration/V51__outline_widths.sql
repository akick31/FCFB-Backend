ALTER TABLE team_field
    ADD COLUMN yard_number_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN end_zone_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN wall_text_outline_width DOUBLE NOT NULL DEFAULT 1.0;

ALTER TABLE game_field
    ADD COLUMN yard_number_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN end_zone_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN wall_text_outline_width DOUBLE NOT NULL DEFAULT 1.0;

ALTER TABLE bowl_field
    ADD COLUMN yard_number_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN end_zone_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN wall_text_outline_width DOUBLE NOT NULL DEFAULT 1.0;

ALTER TABLE playoff_field
    ADD COLUMN yard_number_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN end_zone_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN wall_text_outline_width DOUBLE NOT NULL DEFAULT 1.0;

ALTER TABLE conference_championship_field
    ADD COLUMN yard_number_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN end_zone_outline_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN wall_text_outline_width DOUBLE NOT NULL DEFAULT 1.0;
