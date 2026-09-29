CREATE TABLE bowl_field (
    bowl VARCHAR(255) NOT NULL,
    turf_color VARCHAR(9) NOT NULL DEFAULT '#226633',
    end_zone_fill VARCHAR(16) NOT NULL DEFAULT 'PRIMARY',
    end_zone_font VARCHAR(32) NOT NULL DEFAULT 'CLASSIC',
    left_end_zone_logo_url VARCHAR(512) DEFAULT NULL,
    right_end_zone_logo_url VARCHAR(512) DEFAULT NULL,
    show_conference_logos TINYINT(1) NOT NULL DEFAULT 1,
    yard_number_source VARCHAR(16) NOT NULL DEFAULT 'TEAM_PER_SIDE',
    yard_number_outline_color VARCHAR(9) DEFAULT NULL,
    left_oob_line_color VARCHAR(9) DEFAULT NULL,
    right_oob_line_color VARCHAR(9) DEFAULT NULL,
    red_zone_enabled TINYINT(1) NOT NULL DEFAULT 1,
    red_zone_border_color VARCHAR(9) DEFAULT NULL,
    wall_color VARCHAR(9) DEFAULT NULL,
    wall_design VARCHAR(24) NOT NULL DEFAULT 'TEXT_WITH_LOGOS',
    wall_text VARCHAR(64) DEFAULT NULL,
    wall_text_outline_color VARCHAR(9) DEFAULT NULL,
    goal_post_color VARCHAR(9) NOT NULL DEFAULT '#FFCD00',
    goal_post_style VARCHAR(8) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (bowl)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO bowl_field (bowl, wall_text)
SELECT name, UPPER(name) FROM bowl;
