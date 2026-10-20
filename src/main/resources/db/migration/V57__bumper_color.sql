ALTER TABLE team_uniform_current
    ADD COLUMN bumper_color VARCHAR(9) DEFAULT NULL;

ALTER TABLE team_uniform_history
    ADD COLUMN bumper_color VARCHAR(9) DEFAULT NULL;
