ALTER TABLE team_field
    ADD COLUMN jumbotron_pylon_style VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_pylon_style VARCHAR(32) NULL;

ALTER TABLE game_field
    ADD COLUMN jumbotron_pylon_style VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_pylon_style VARCHAR(32) NULL;
