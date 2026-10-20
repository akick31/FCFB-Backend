ALTER TABLE team_field
    ADD COLUMN right_stadium_backdrop VARCHAR(16) DEFAULT NULL,
    ADD COLUMN right_stadium_size DOUBLE DEFAULT NULL;

ALTER TABLE game_field
    ADD COLUMN right_stadium_backdrop VARCHAR(16) DEFAULT NULL,
    ADD COLUMN right_stadium_size DOUBLE DEFAULT NULL;
