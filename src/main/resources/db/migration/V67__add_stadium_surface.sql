ALTER TABLE venue
    ADD COLUMN surface VARCHAR(16) NULL,
    ADD COLUMN mowing_pattern VARCHAR(16) NULL;

ALTER TABLE game_look
    ADD COLUMN surface VARCHAR(16) NULL,
    ADD COLUMN mowing_pattern VARCHAR(16) NULL;
