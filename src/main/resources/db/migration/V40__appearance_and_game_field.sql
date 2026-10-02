-- Logo rotation + away-jersey number colors (team_uniform_current/history)
ALTER TABLE team_uniform_current
    ADD COLUMN logo_rotation DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN away_number_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN away_number_outline_color VARCHAR(9) DEFAULT NULL;

ALTER TABLE team_uniform_history
    ADD COLUMN logo_rotation DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN away_number_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN away_number_outline_color VARCHAR(9) DEFAULT NULL;

-- Per-side end-zone text, end-zone side logo, and no-outline flag (team_field)
ALTER TABLE team_field
    ADD COLUMN end_zone_text_left VARCHAR(64) DEFAULT NULL,
    ADD COLUMN end_zone_text_right VARCHAR(64) DEFAULT NULL,
    ADD COLUMN end_zone_logo_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN end_zone_logo_source VARCHAR(16) NOT NULL DEFAULT 'PRIMARY',
    ADD COLUMN end_zone_logo_url VARCHAR(512) DEFAULT NULL,
    ADD COLUMN end_zone_outline_enabled TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN end_zone_logo_size DOUBLE NOT NULL DEFAULT 0.85;

-- Secondary-helmet full config: alt_* columns (team_uniform_current/history)
ALTER TABLE team_uniform_current
    ADD COLUMN alt_facemask_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN alt_helmet_number_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN alt_helmet_logo_mode VARCHAR(16) DEFAULT NULL,
    ADD COLUMN alt_helmet_logo_source VARCHAR(16) DEFAULT NULL,
    ADD COLUMN alt_has_logo TINYINT(1) DEFAULT NULL,
    ADD COLUMN alt_logo_url VARCHAR(512) DEFAULT NULL,
    ADD COLUMN alt_logo_size DOUBLE DEFAULT NULL,
    ADD COLUMN alt_logo_x DOUBLE DEFAULT NULL,
    ADD COLUMN alt_logo_y DOUBLE DEFAULT NULL,
    ADD COLUMN alt_logo_rotation DOUBLE DEFAULT NULL,
    ADD COLUMN alt_has_stripe TINYINT(1) DEFAULT NULL,
    ADD COLUMN alt_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN alt_stripe_type VARCHAR(16) DEFAULT NULL,
    ADD COLUMN alt_secondary_stripe_color VARCHAR(9) DEFAULT NULL;

ALTER TABLE team_uniform_history
    ADD COLUMN alt_facemask_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN alt_helmet_number_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN alt_helmet_logo_mode VARCHAR(16) DEFAULT NULL,
    ADD COLUMN alt_helmet_logo_source VARCHAR(16) DEFAULT NULL,
    ADD COLUMN alt_has_logo TINYINT(1) DEFAULT NULL,
    ADD COLUMN alt_logo_url VARCHAR(512) DEFAULT NULL,
    ADD COLUMN alt_logo_size DOUBLE DEFAULT NULL,
    ADD COLUMN alt_logo_x DOUBLE DEFAULT NULL,
    ADD COLUMN alt_logo_y DOUBLE DEFAULT NULL,
    ADD COLUMN alt_logo_rotation DOUBLE DEFAULT NULL,
    ADD COLUMN alt_has_stripe TINYINT(1) DEFAULT NULL,
    ADD COLUMN alt_stripe_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN alt_stripe_type VARCHAR(16) DEFAULT NULL,
    ADD COLUMN alt_secondary_stripe_color VARCHAR(9) DEFAULT NULL;

-- Wall logo source (team_field)
ALTER TABLE team_field
    ADD COLUMN wall_logo_source VARCHAR(16) NOT NULL DEFAULT 'NONE',
    ADD COLUMN wall_logo_url VARCHAR(512) DEFAULT NULL;

-- Postseason per-side colors/text/logos (playoff_field + conference_championship_field)
ALTER TABLE playoff_field
    ADD COLUMN left_sideline_color VARCHAR(16) DEFAULT NULL,
    ADD COLUMN right_sideline_color VARCHAR(16) DEFAULT NULL,
    ADD COLUMN left_red_zone_color VARCHAR(16) DEFAULT NULL,
    ADD COLUMN right_red_zone_color VARCHAR(16) DEFAULT NULL,
    ADD COLUMN left_end_zone_text VARCHAR(64) DEFAULT NULL,
    ADD COLUMN right_end_zone_text VARCHAR(64) DEFAULT NULL,
    ADD COLUMN left_end_zone_logo_url VARCHAR(512) DEFAULT NULL,
    ADD COLUMN right_end_zone_logo_url VARCHAR(512) DEFAULT NULL;

ALTER TABLE conference_championship_field
    ADD COLUMN left_sideline_color VARCHAR(16) DEFAULT NULL,
    ADD COLUMN right_sideline_color VARCHAR(16) DEFAULT NULL,
    ADD COLUMN left_red_zone_color VARCHAR(16) DEFAULT NULL,
    ADD COLUMN right_red_zone_color VARCHAR(16) DEFAULT NULL,
    ADD COLUMN left_end_zone_text VARCHAR(64) DEFAULT NULL,
    ADD COLUMN right_end_zone_text VARCHAR(64) DEFAULT NULL,
    ADD COLUMN left_end_zone_logo_url VARCHAR(512) DEFAULT NULL,
    ADD COLUMN right_end_zone_logo_url VARCHAR(512) DEFAULT NULL;

-- Per-game frozen field appearance (new game_field table)
CREATE TABLE IF NOT EXISTS game_field (
    game_id INT NOT NULL PRIMARY KEY,
    turf_color VARCHAR(255) NOT NULL DEFAULT '#226633',
    end_zone_color VARCHAR(255) DEFAULT NULL,
    end_zone_text_color VARCHAR(255) DEFAULT NULL,
    end_zone_outline_color VARCHAR(255) DEFAULT NULL,
    end_zone_text_left VARCHAR(64) DEFAULT NULL,
    end_zone_text_right VARCHAR(64) DEFAULT NULL,
    end_zone_logo_enabled TINYINT(1) NOT NULL DEFAULT 0,
    end_zone_logo_source VARCHAR(16) NOT NULL DEFAULT 'PRIMARY',
    end_zone_logo_url VARCHAR(512) DEFAULT NULL,
    end_zone_logo_size DOUBLE NOT NULL DEFAULT 0.85,
    end_zone_outline_enabled TINYINT(1) NOT NULL DEFAULT 1,
    wall_logo_source VARCHAR(16) NOT NULL DEFAULT 'NONE',
    wall_logo_url VARCHAR(512) DEFAULT NULL,
    end_zone_font VARCHAR(255) NOT NULL DEFAULT 'CLASSIC',
    midfield_logo_url VARCHAR(512) DEFAULT NULL,
    midfield_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM',
    field_number_outline_color VARCHAR(255) DEFAULT NULL,
    red_zone_border_color VARCHAR(255) DEFAULT NULL,
    oob_line_color VARCHAR(255) DEFAULT NULL,
    wall_color VARCHAR(255) DEFAULT NULL,
    wall_design VARCHAR(255) NOT NULL DEFAULT 'REPEATING_LOGOS',
    wall_text VARCHAR(255) DEFAULT NULL,
    wall_text_outline_color VARCHAR(255) DEFAULT NULL,
    goal_post_color VARCHAR(255) NOT NULL DEFAULT '#FFCD00',
    goal_post_style VARCHAR(255) NOT NULL DEFAULT 'Y',
    quarter_logo_url VARCHAR(512) DEFAULT NULL,
    quarter_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM'
);
