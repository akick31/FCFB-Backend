ALTER TABLE venue
    DROP COLUMN surface,
    DROP COLUMN mowing_pattern;

ALTER TABLE game_look
    DROP COLUMN surface,
    DROP COLUMN mowing_pattern;

ALTER TABLE team_field
    ADD COLUMN surface VARCHAR(16) NULL,
    ADD COLUMN mowing_pattern VARCHAR(16) NULL;

ALTER TABLE game_field
    ADD COLUMN surface VARCHAR(16) NULL,
    ADD COLUMN mowing_pattern VARCHAR(16) NULL;
