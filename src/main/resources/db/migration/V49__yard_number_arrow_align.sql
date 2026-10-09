ALTER TABLE team_field
    ADD COLUMN yard_number_arrow_align VARCHAR(16) DEFAULT NULL;

ALTER TABLE game_field
    ADD COLUMN yard_number_arrow_align VARCHAR(16) DEFAULT NULL;

ALTER TABLE bowl_field
    ADD COLUMN yard_number_arrow_align VARCHAR(16) DEFAULT NULL;

ALTER TABLE playoff_field
    ADD COLUMN yard_number_arrow_align VARCHAR(16) DEFAULT NULL;

ALTER TABLE conference_championship_field
    ADD COLUMN yard_number_arrow_align VARCHAR(16) DEFAULT NULL;
