ALTER TABLE team_uniform_current
    ADD COLUMN IF NOT EXISTS shoulder_stripe_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS secondary_shoulder_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS sleeve_stripe_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS sleeve_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS secondary_sleeve_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_shoulder_stripe_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_shoulder_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_secondary_shoulder_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_sleeve_stripe_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_sleeve_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_secondary_sleeve_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_number_top_text VARCHAR(32) DEFAULT NULL;

ALTER TABLE team_uniform_history
    ADD COLUMN IF NOT EXISTS shoulder_stripe_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS secondary_shoulder_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS sleeve_stripe_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS sleeve_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS secondary_sleeve_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_shoulder_stripe_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_shoulder_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_secondary_shoulder_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_sleeve_stripe_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_sleeve_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_secondary_sleeve_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS away_number_top_text VARCHAR(32) DEFAULT NULL;
