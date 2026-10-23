ALTER TABLE team_field
    ADD COLUMN jumbotron_elevation DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN right_jumbotron_elevation DOUBLE NOT NULL DEFAULT 0.0;

ALTER TABLE game_field
    ADD COLUMN jumbotron_elevation DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN right_jumbotron_elevation DOUBLE NOT NULL DEFAULT 0.0;
