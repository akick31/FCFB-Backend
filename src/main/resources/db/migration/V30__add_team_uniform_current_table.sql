CREATE TABLE team_uniform_current (
    team VARCHAR(255) NOT NULL,
    primary_color VARCHAR(9) DEFAULT NULL,
    secondary_color VARCHAR(9) DEFAULT NULL,
    tertiary_color VARCHAR(9) DEFAULT NULL,
    helmet_color VARCHAR(9) DEFAULT NULL,
    secondary_helmet_color VARCHAR(9) DEFAULT NULL,
    helmet_number_color VARCHAR(9) NOT NULL DEFAULT '#FFFFFF',
    facemask_color VARCHAR(9) NOT NULL DEFAULT '#FFFFFF',
    helmet_logo_mode VARCHAR(16) NOT NULL DEFAULT 'MAIN',
    logo_url VARCHAR(512) DEFAULT NULL,
    has_logo TINYINT(1) NOT NULL DEFAULT 1,
    has_stripe TINYINT(1) NOT NULL DEFAULT 0,
    stripe_color VARCHAR(9) DEFAULT NULL,
    jersey_color VARCHAR(9) DEFAULT NULL,
    number_color VARCHAR(9) DEFAULT NULL,
    number_outline_color VARCHAR(9) DEFAULT NULL,
    pants_color VARCHAR(9) DEFAULT NULL,
    PRIMARY KEY (team)
);

INSERT INTO team_uniform_current (
    team,
    primary_color,
    secondary_color,
    jersey_color,
    pants_color,
    number_color,
    helmet_number_color,
    logo_url
)
SELECT
    name,
    primary_color,
    secondary_color,
    primary_color,
    primary_color,
    '#FFFFFF',
    '#FFFFFF',
    scorebug_logo
FROM team
WHERE name IS NOT NULL;
