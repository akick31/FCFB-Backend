CREATE TABLE team_field (
    team VARCHAR(255) NOT NULL,
    turf_color VARCHAR(9) NOT NULL DEFAULT '#226633',
    end_zone_color VARCHAR(9) DEFAULT NULL,
    end_zone_font VARCHAR(32) NOT NULL DEFAULT 'CLASSIC',
    midfield_logo_url VARCHAR(512) DEFAULT NULL,
    field_number_outline_color VARCHAR(9) DEFAULT NULL,
    red_zone_border_color VARCHAR(9) DEFAULT NULL,
    oob_line_color VARCHAR(9) DEFAULT NULL,
    wall_color VARCHAR(9) DEFAULT NULL,
    wall_design VARCHAR(24) NOT NULL DEFAULT 'REPEATING_LOGOS',
    wall_text VARCHAR(64) DEFAULT NULL,
    wall_text_outline_color VARCHAR(9) DEFAULT NULL,
    goal_post_color VARCHAR(9) NOT NULL DEFAULT '#FFCD00',
    goal_post_style VARCHAR(8) NOT NULL DEFAULT 'Y',
    quarter_logo_url VARCHAR(512) DEFAULT NULL,
    PRIMARY KEY (team)
);

INSERT INTO team_field (team, turf_color, end_zone_color, midfield_logo_url, red_zone_border_color, wall_color)
SELECT name, '#226633', primary_color, scorebug_logo, primary_color, primary_color
FROM team
WHERE name IS NOT NULL;

UPDATE team_field SET turf_color = '#0033A0', end_zone_color = '#D64309' WHERE team = 'Boise State';
UPDATE team_field SET turf_color = '#006F71', end_zone_color = '#A27752' WHERE team = 'Coastal Carolina';
UPDATE team_field SET turf_color = '#6E6E70', end_zone_color = '#006633' WHERE team = 'Eastern Michigan';
