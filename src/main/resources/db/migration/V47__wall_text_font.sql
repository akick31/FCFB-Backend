ALTER TABLE team_field
    ADD COLUMN wall_text_font VARCHAR(32) DEFAULT NULL;

ALTER TABLE game_field
    ADD COLUMN wall_text_font VARCHAR(32) DEFAULT NULL;

ALTER TABLE bowl_field
    ADD COLUMN wall_text_font VARCHAR(32) DEFAULT NULL;

ALTER TABLE playoff_field
    ADD COLUMN wall_text_font VARCHAR(32) DEFAULT NULL;

ALTER TABLE conference_championship_field
    ADD COLUMN wall_text_font VARCHAR(32) DEFAULT NULL;
