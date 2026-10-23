ALTER TABLE team_field
    ADD COLUMN skyline_backdrop VARCHAR(32) NULL,
    ADD COLUMN right_skyline_backdrop VARCHAR(32) NULL;

ALTER TABLE game_field
    ADD COLUMN skyline_backdrop VARCHAR(32) NULL,
    ADD COLUMN right_skyline_backdrop VARCHAR(32) NULL;

UPDATE team_field SET backdrop_style = 'FACILITY_BRICK' WHERE backdrop_style IN ('FACILITY_WIDE', 'FACILITY_TALL');
UPDATE team_field SET right_backdrop_style = 'FACILITY_BRICK' WHERE right_backdrop_style IN ('FACILITY_WIDE', 'FACILITY_TALL');
UPDATE team_field SET backdrop_style = 'FACILITY_NARROW_GEORGIAN' WHERE backdrop_style = 'FACILITY_SHORT';
UPDATE team_field SET right_backdrop_style = 'FACILITY_NARROW_GEORGIAN' WHERE right_backdrop_style = 'FACILITY_SHORT';
UPDATE game_field SET backdrop_style = 'FACILITY_BRICK' WHERE backdrop_style IN ('FACILITY_WIDE', 'FACILITY_TALL');
UPDATE game_field SET right_backdrop_style = 'FACILITY_BRICK' WHERE right_backdrop_style IN ('FACILITY_WIDE', 'FACILITY_TALL');
UPDATE game_field SET backdrop_style = 'FACILITY_NARROW_GEORGIAN' WHERE backdrop_style = 'FACILITY_SHORT';
UPDATE game_field SET right_backdrop_style = 'FACILITY_NARROW_GEORGIAN' WHERE right_backdrop_style = 'FACILITY_SHORT';
