RENAME TABLE team_uniform TO team_uniform_history;

ALTER TABLE team_uniform_history
    ADD COLUMN primary_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN secondary_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN tertiary_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN helmet_logo_mode VARCHAR(16) NOT NULL DEFAULT 'MAIN',
    ADD COLUMN secondary_helmet_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN helmet_number_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN number_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN number_outline_color VARCHAR(9) DEFAULT NULL;
