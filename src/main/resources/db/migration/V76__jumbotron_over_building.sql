ALTER TABLE team_field
    ADD COLUMN jumbotron_over_building BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN right_jumbotron_over_building BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE game_field
    ADD COLUMN jumbotron_over_building BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN right_jumbotron_over_building BOOLEAN NOT NULL DEFAULT FALSE;
