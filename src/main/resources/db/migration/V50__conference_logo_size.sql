ALTER TABLE team_field
    ADD COLUMN conference_logo_size DOUBLE NOT NULL DEFAULT 1.0;

ALTER TABLE game_field
    ADD COLUMN conference_logo_size DOUBLE NOT NULL DEFAULT 1.0;

ALTER TABLE bowl_field
    ADD COLUMN conference_logo_size DOUBLE NOT NULL DEFAULT 1.0;
