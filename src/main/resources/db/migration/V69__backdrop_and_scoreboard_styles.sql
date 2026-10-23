ALTER TABLE team_field
    ADD COLUMN backdrop_style VARCHAR(32) NULL,
    ADD COLUMN right_backdrop_style VARCHAR(32) NULL,
    ADD COLUMN jumbotron_style VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_style VARCHAR(32) NULL;

ALTER TABLE game_field
    ADD COLUMN backdrop_style VARCHAR(32) NULL,
    ADD COLUMN right_backdrop_style VARCHAR(32) NULL,
    ADD COLUMN jumbotron_style VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_style VARCHAR(32) NULL;
