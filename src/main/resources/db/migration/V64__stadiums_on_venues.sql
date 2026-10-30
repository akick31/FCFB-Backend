ALTER TABLE team_uniform_current
    ADD COLUMN bumper_color VARCHAR(9) DEFAULT NULL;

ALTER TABLE team_uniform_history
    ADD COLUMN bumper_color VARCHAR(9) DEFAULT NULL;

ALTER TABLE venue
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
    ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN stadium_backdrop VARCHAR(32) NULL,
    ADD COLUMN stadium_size DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN right_stadium_backdrop VARCHAR(32) NULL,
    ADD COLUMN right_stadium_size DOUBLE NULL,
    ADD COLUMN jumbotron_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_jumbotron_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN jumbotron_pylon_mount TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_jumbotron_pylon_mount TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN jumbotron_size DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN right_jumbotron_size DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN wall_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_wall_enabled TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN jumbotron_elevation DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN right_jumbotron_elevation DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN skyline_backdrop VARCHAR(32) NULL,
    ADD COLUMN right_skyline_backdrop VARCHAR(32) NULL,
    ADD COLUMN jumbotron_shape VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_shape VARCHAR(32) NULL,
    ADD COLUMN jumbotron_pylons_outside TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_jumbotron_pylons_outside TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN jumbotron_color VARCHAR(16) NULL,
    ADD COLUMN right_jumbotron_color VARCHAR(16) NULL,
    ADD COLUMN topper_color VARCHAR(16) NULL,
    ADD COLUMN right_topper_color VARCHAR(16) NULL,
    ADD COLUMN facility_full_width TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_facility_full_width TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN jumbotron_over_building TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN right_jumbotron_over_building TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN scoreboard_logo VARCHAR(16) NULL,
    ADD COLUMN right_scoreboard_logo VARCHAR(16) NULL,
    ADD COLUMN jumbotron_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN right_jumbotron_width DOUBLE NOT NULL DEFAULT 1.0,
    ADD COLUMN jumbotron_pylon_style VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_pylon_style VARCHAR(32) NULL,
    ADD COLUMN backdrop_style VARCHAR(32) NULL,
    ADD COLUMN right_backdrop_style VARCHAR(32) NULL,
    ADD COLUMN jumbotron_style VARCHAR(32) NULL,
    ADD COLUMN right_jumbotron_style VARCHAR(32) NULL,
    ADD COLUMN country VARCHAR(64) NOT NULL DEFAULT 'United States';

INSERT IGNORE INTO venue (name)
SELECT DISTINCT stadium FROM team WHERE stadium IS NOT NULL AND TRIM(stadium) <> '';

INSERT IGNORE INTO venue (name)
SELECT CONCAT(name, ' Stadium') FROM team WHERE stadium IS NULL OR TRIM(stadium) = '';

UPDATE team SET stadium = CONCAT(name, ' Stadium') WHERE stadium IS NULL OR TRIM(stadium) = '';

ALTER TABLE bowl_field
    ADD COLUMN venue VARCHAR(255) NULL;

ALTER TABLE playoff_field
    ADD COLUMN venue VARCHAR(255) NULL;

CREATE TABLE game_look (
    game_id INT NOT NULL,
    stadium_captured TINYINT(1) NOT NULL DEFAULT 0,
    stadium_backdrop VARCHAR(32) NULL,
    stadium_size DOUBLE NOT NULL DEFAULT 1.0,
    right_stadium_backdrop VARCHAR(32) NULL,
    right_stadium_size DOUBLE NULL,
    jumbotron_enabled TINYINT(1) NOT NULL DEFAULT 0,
    right_jumbotron_enabled TINYINT(1) NOT NULL DEFAULT 0,
    jumbotron_pylon_mount TINYINT(1) NOT NULL DEFAULT 0,
    right_jumbotron_pylon_mount TINYINT(1) NOT NULL DEFAULT 0,
    jumbotron_size DOUBLE NOT NULL DEFAULT 1.0,
    right_jumbotron_size DOUBLE NOT NULL DEFAULT 1.0,
    wall_enabled TINYINT(1) NOT NULL DEFAULT 0,
    right_wall_enabled TINYINT(1) NOT NULL DEFAULT 0,
    jumbotron_elevation DOUBLE NOT NULL DEFAULT 0.0,
    right_jumbotron_elevation DOUBLE NOT NULL DEFAULT 0.0,
    skyline_backdrop VARCHAR(32) NULL,
    right_skyline_backdrop VARCHAR(32) NULL,
    jumbotron_shape VARCHAR(32) NULL,
    right_jumbotron_shape VARCHAR(32) NULL,
    jumbotron_pylons_outside TINYINT(1) NOT NULL DEFAULT 0,
    right_jumbotron_pylons_outside TINYINT(1) NOT NULL DEFAULT 0,
    jumbotron_color VARCHAR(16) NULL,
    right_jumbotron_color VARCHAR(16) NULL,
    topper_color VARCHAR(16) NULL,
    right_topper_color VARCHAR(16) NULL,
    facility_full_width TINYINT(1) NOT NULL DEFAULT 0,
    right_facility_full_width TINYINT(1) NOT NULL DEFAULT 0,
    jumbotron_over_building TINYINT(1) NOT NULL DEFAULT 0,
    right_jumbotron_over_building TINYINT(1) NOT NULL DEFAULT 0,
    scoreboard_logo VARCHAR(16) NULL,
    right_scoreboard_logo VARCHAR(16) NULL,
    jumbotron_width DOUBLE NOT NULL DEFAULT 1.0,
    right_jumbotron_width DOUBLE NOT NULL DEFAULT 1.0,
    jumbotron_pylon_style VARCHAR(32) NULL,
    right_jumbotron_pylon_style VARCHAR(32) NULL,
    backdrop_style VARCHAR(32) NULL,
    right_backdrop_style VARCHAR(32) NULL,
    jumbotron_style VARCHAR(32) NULL,
    right_jumbotron_style VARCHAR(32) NULL,
    bowl_field_json LONGTEXT NULL,
    postseason_field_json LONGTEXT NULL,
    postseason_kind VARCHAR(16) NULL,
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (game_id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE stadium_change_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    venue VARCHAR(255) NOT NULL,
    action VARCHAR(16) NOT NULL,
    summary VARCHAR(512) NULL,
    changed_by VARCHAR(255) NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    before_json LONGTEXT NULL,
    PRIMARY KEY (id),
    KEY idx_stadium_change_log_venue (venue, changed_at)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
