ALTER TABLE team_field
    ADD COLUMN jumbotron_shape VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_shape VARCHAR(32) NULL,
    ADD COLUMN jumbotron_pylons_outside BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN right_jumbotron_pylons_outside BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN facility_mount VARCHAR(32) NULL,
    ADD COLUMN right_facility_mount VARCHAR(32) NULL;

ALTER TABLE game_field
    ADD COLUMN jumbotron_shape VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_shape VARCHAR(32) NULL,
    ADD COLUMN jumbotron_pylons_outside BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN right_jumbotron_pylons_outside BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN facility_mount VARCHAR(32) NULL,
    ADD COLUMN right_facility_mount VARCHAR(32) NULL;

UPDATE team_field SET jumbotron_pylons_outside = TRUE WHERE jumbotron_pylon_style = 'OUTSIDE';
UPDATE team_field SET right_jumbotron_pylons_outside = TRUE WHERE right_jumbotron_pylon_style = 'OUTSIDE';
UPDATE team_field SET jumbotron_pylon_style = 'STEEL' WHERE jumbotron_pylon_style IN ('TWIN', 'OUTSIDE');
UPDATE team_field SET right_jumbotron_pylon_style = 'STEEL' WHERE right_jumbotron_pylon_style IN ('TWIN', 'OUTSIDE');
UPDATE game_field SET jumbotron_pylons_outside = TRUE WHERE jumbotron_pylon_style = 'OUTSIDE';
UPDATE game_field SET right_jumbotron_pylons_outside = TRUE WHERE right_jumbotron_pylon_style = 'OUTSIDE';
UPDATE game_field SET jumbotron_pylon_style = 'STEEL' WHERE jumbotron_pylon_style IN ('TWIN', 'OUTSIDE');
UPDATE game_field SET right_jumbotron_pylon_style = 'STEEL' WHERE right_jumbotron_pylon_style IN ('TWIN', 'OUTSIDE');

UPDATE team_field SET jumbotron_enabled = TRUE WHERE stadium_backdrop = 'FACILITY';
UPDATE team_field SET right_jumbotron_enabled = TRUE WHERE COALESCE(right_stadium_backdrop, stadium_backdrop) = 'FACILITY';
UPDATE game_field SET jumbotron_enabled = TRUE WHERE stadium_backdrop = 'FACILITY';
UPDATE game_field SET right_jumbotron_enabled = TRUE WHERE COALESCE(right_stadium_backdrop, stadium_backdrop) = 'FACILITY';

UPDATE team_field SET facility_mount = 'ROOF', backdrop_style = 'FACILITY_SHORT' WHERE backdrop_style = 'FACILITY_ROOF';
UPDATE team_field SET facility_mount = 'PYLONS', backdrop_style = 'FACILITY_WIDE' WHERE backdrop_style = 'FACILITY_GANTRY';
UPDATE team_field SET facility_mount = 'WALL', backdrop_style = 'FACILITY_WIDE' WHERE backdrop_style = 'FACILITY_WALL';
UPDATE team_field SET right_facility_mount = 'ROOF', right_backdrop_style = 'FACILITY_SHORT' WHERE right_backdrop_style = 'FACILITY_ROOF';
UPDATE team_field SET right_facility_mount = 'PYLONS', right_backdrop_style = 'FACILITY_WIDE' WHERE right_backdrop_style = 'FACILITY_GANTRY';
UPDATE team_field SET right_facility_mount = 'WALL', right_backdrop_style = 'FACILITY_WIDE' WHERE right_backdrop_style = 'FACILITY_WALL';
UPDATE game_field SET facility_mount = 'ROOF', backdrop_style = 'FACILITY_SHORT' WHERE backdrop_style = 'FACILITY_ROOF';
UPDATE game_field SET facility_mount = 'PYLONS', backdrop_style = 'FACILITY_WIDE' WHERE backdrop_style = 'FACILITY_GANTRY';
UPDATE game_field SET facility_mount = 'WALL', backdrop_style = 'FACILITY_WIDE' WHERE backdrop_style = 'FACILITY_WALL';
UPDATE game_field SET right_facility_mount = 'ROOF', right_backdrop_style = 'FACILITY_SHORT' WHERE right_backdrop_style = 'FACILITY_ROOF';
UPDATE game_field SET right_facility_mount = 'PYLONS', right_backdrop_style = 'FACILITY_WIDE' WHERE right_backdrop_style = 'FACILITY_GANTRY';
UPDATE game_field SET right_facility_mount = 'WALL', right_backdrop_style = 'FACILITY_WIDE' WHERE right_backdrop_style = 'FACILITY_WALL';
