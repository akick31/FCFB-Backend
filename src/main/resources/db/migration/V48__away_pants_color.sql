ALTER TABLE team_uniform_current
    ADD COLUMN away_pants_color VARCHAR(9) DEFAULT NULL;

ALTER TABLE team_uniform_history
    ADD COLUMN away_pants_color VARCHAR(9) DEFAULT NULL;
