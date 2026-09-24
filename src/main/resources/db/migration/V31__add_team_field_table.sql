CREATE TABLE team_field (
    team VARCHAR(255) NOT NULL,
    turf_color VARCHAR(9) NOT NULL DEFAULT '#226633',
    end_zone_color VARCHAR(9) DEFAULT NULL,
    end_zone_font VARCHAR(32) NOT NULL DEFAULT 'CLASSIC',
    midfield_logo_url VARCHAR(512) DEFAULT NULL,
    field_number_outline_color VARCHAR(9) DEFAULT NULL,
    quarter_logo_url VARCHAR(512) DEFAULT NULL,
    PRIMARY KEY (team)
);

INSERT INTO team_field (team, turf_color, end_zone_color, midfield_logo_url)
SELECT name, '#226633', primary_color, scorebug_logo
FROM team
WHERE name IS NOT NULL;

UPDATE team_field SET turf_color = '#0033A0', end_zone_color = '#D64309' WHERE team = 'Boise State';
UPDATE team_field SET turf_color = '#006F71', end_zone_color = '#A27752' WHERE team = 'Coastal Carolina';
UPDATE team_field SET turf_color = '#6E6E70', end_zone_color = '#006633' WHERE team = 'Eastern Michigan';
