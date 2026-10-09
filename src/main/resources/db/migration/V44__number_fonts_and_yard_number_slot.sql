ALTER TABLE team_uniform_history
    ADD COLUMN helmet_number_font VARCHAR(32) NULL,
    ADD COLUMN jersey_number_font VARCHAR(32) NULL;

ALTER TABLE team_uniform_current
    ADD COLUMN helmet_number_font VARCHAR(32) NULL,
    ADD COLUMN jersey_number_font VARCHAR(32) NULL;

ALTER TABLE bowl_field
    ADD COLUMN yard_number_team_slot VARCHAR(16) NULL;

ALTER TABLE playoff_field
    ADD COLUMN yard_number_team_slot VARCHAR(16) NULL;

ALTER TABLE conference_championship_field
    ADD COLUMN yard_number_team_slot VARCHAR(16) NULL;
