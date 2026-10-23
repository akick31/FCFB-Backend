ALTER TABLE team_field
    ADD COLUMN jumbotron_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN right_jumbotron_width DOUBLE NOT NULL DEFAULT 1.0;

ALTER TABLE game_field
    ADD COLUMN jumbotron_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN right_jumbotron_width DOUBLE NOT NULL DEFAULT 1.0;
