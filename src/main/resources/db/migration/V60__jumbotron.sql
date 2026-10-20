ALTER TABLE team_field
    ADD COLUMN jumbotron_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_jumbotron_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN jumbotron_pylon_mount TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_jumbotron_pylon_mount TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE game_field
    ADD COLUMN jumbotron_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_jumbotron_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN jumbotron_pylon_mount TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_jumbotron_pylon_mount TINYINT(1) NOT NULL DEFAULT 0;
