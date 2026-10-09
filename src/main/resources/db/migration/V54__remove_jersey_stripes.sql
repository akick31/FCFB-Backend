ALTER TABLE team_uniform_current
    DROP COLUMN IF EXISTS has_shoulder_stripe,
    DROP COLUMN IF EXISTS shoulder_stripe_color,
    DROP COLUMN IF EXISTS shoulder_stripe_type,
    DROP COLUMN IF EXISTS secondary_shoulder_stripe_color,
    DROP COLUMN IF EXISTS sleeve_stripe_type,
    DROP COLUMN IF EXISTS sleeve_stripe_color,
    DROP COLUMN IF EXISTS secondary_sleeve_stripe_color,
    DROP COLUMN IF EXISTS away_shoulder_stripe_type,
    DROP COLUMN IF EXISTS away_shoulder_stripe_color,
    DROP COLUMN IF EXISTS away_secondary_shoulder_stripe_color,
    DROP COLUMN IF EXISTS away_sleeve_stripe_type,
    DROP COLUMN IF EXISTS away_sleeve_stripe_color,
    DROP COLUMN IF EXISTS away_secondary_sleeve_stripe_color;

ALTER TABLE team_uniform_history
    DROP COLUMN IF EXISTS has_shoulder_stripe,
    DROP COLUMN IF EXISTS shoulder_stripe_color,
    DROP COLUMN IF EXISTS shoulder_stripe_type,
    DROP COLUMN IF EXISTS secondary_shoulder_stripe_color,
    DROP COLUMN IF EXISTS sleeve_stripe_type,
    DROP COLUMN IF EXISTS sleeve_stripe_color,
    DROP COLUMN IF EXISTS secondary_sleeve_stripe_color,
    DROP COLUMN IF EXISTS away_shoulder_stripe_type,
    DROP COLUMN IF EXISTS away_shoulder_stripe_color,
    DROP COLUMN IF EXISTS away_secondary_shoulder_stripe_color,
    DROP COLUMN IF EXISTS away_sleeve_stripe_type,
    DROP COLUMN IF EXISTS away_sleeve_stripe_color,
    DROP COLUMN IF EXISTS away_secondary_sleeve_stripe_color;
