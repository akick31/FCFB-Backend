CREATE TABLE playoff_field (
    round VARCHAR(64) NOT NULL,
    turf_color VARCHAR(9) NOT NULL DEFAULT '#226633',
    end_zone_font VARCHAR(32) NOT NULL DEFAULT 'CLASSIC',
    center_logo_url VARCHAR(512) DEFAULT NULL,
    wall_color VARCHAR(9) DEFAULT NULL,
    wall_design VARCHAR(24) NOT NULL DEFAULT 'TEXT_WITH_LOGOS',
    wall_text VARCHAR(64) DEFAULT NULL,
    wall_text_outline_color VARCHAR(9) DEFAULT NULL,
    goal_post_color VARCHAR(9) NOT NULL DEFAULT '#FFCD00',
    goal_post_style VARCHAR(8) NOT NULL DEFAULT 'Y',
    yard_number_outline_color VARCHAR(9) DEFAULT NULL,
    red_zone_border_color VARCHAR(9) DEFAULT NULL,
    sideline_accent_color VARCHAR(9) DEFAULT NULL,
    PRIMARY KEY (round)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO playoff_field (round) VALUES
    ('First Round'),
    ('Second Round'),
    ('Quarterfinal'),
    ('Semifinal'),
    ('National Championship');

CREATE TABLE conference_championship_field (
    conference VARCHAR(64) NOT NULL,
    turf_color VARCHAR(9) NOT NULL DEFAULT '#226633',
    end_zone_font VARCHAR(32) NOT NULL DEFAULT 'CLASSIC',
    center_logo_url VARCHAR(512) DEFAULT NULL,
    wall_color VARCHAR(9) DEFAULT NULL,
    wall_design VARCHAR(24) NOT NULL DEFAULT 'TEXT_WITH_LOGOS',
    wall_text VARCHAR(64) DEFAULT NULL,
    wall_text_outline_color VARCHAR(9) DEFAULT NULL,
    goal_post_color VARCHAR(9) NOT NULL DEFAULT '#FFCD00',
    goal_post_style VARCHAR(8) NOT NULL DEFAULT 'Y',
    yard_number_outline_color VARCHAR(9) DEFAULT NULL,
    red_zone_border_color VARCHAR(9) DEFAULT NULL,
    sideline_accent_color VARCHAR(9) DEFAULT NULL,
    PRIMARY KEY (conference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO conference_championship_field (conference)
SELECT code FROM conference;
