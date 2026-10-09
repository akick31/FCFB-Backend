ALTER TABLE team_uniform_current
    ADD COLUMN stripe_type VARCHAR(16) NOT NULL DEFAULT 'SINGLE',
    ADD COLUMN secondary_stripe_color VARCHAR(9) DEFAULT NULL;

ALTER TABLE team_uniform_history
    ADD COLUMN stripe_type VARCHAR(16) NOT NULL DEFAULT 'SINGLE',
    ADD COLUMN secondary_stripe_color VARCHAR(9) DEFAULT NULL;
