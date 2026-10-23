ALTER TABLE team_field
    ADD COLUMN scoreboard_logo VARCHAR(16) NULL,
    ADD COLUMN right_scoreboard_logo VARCHAR(16) NULL,
    DROP COLUMN facility_mount,
    DROP COLUMN right_facility_mount;

ALTER TABLE game_field
    ADD COLUMN scoreboard_logo VARCHAR(16) NULL,
    ADD COLUMN right_scoreboard_logo VARCHAR(16) NULL,
    DROP COLUMN facility_mount,
    DROP COLUMN right_facility_mount;

UPDATE team_field SET backdrop_style = 'FACILITY_BRICK' WHERE backdrop_style = 'FACILITY_GLASS';
UPDATE team_field SET right_backdrop_style = 'FACILITY_BRICK' WHERE right_backdrop_style = 'FACILITY_GLASS';
UPDATE game_field SET backdrop_style = 'FACILITY_BRICK' WHERE backdrop_style = 'FACILITY_GLASS';
UPDATE game_field SET right_backdrop_style = 'FACILITY_BRICK' WHERE right_backdrop_style = 'FACILITY_GLASS';
