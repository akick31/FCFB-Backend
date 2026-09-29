CREATE TABLE team_uniform (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team VARCHAR(255) NOT NULL,
    season_number INT NOT NULL,
    week INT NOT NULL,
    helmet_color VARCHAR(9) DEFAULT NULL,
    facemask_color VARCHAR(9) DEFAULT NULL,
    jersey_color VARCHAR(9) DEFAULT NULL,
    pants_color VARCHAR(9) DEFAULT NULL,
    logo_url VARCHAR(512) DEFAULT NULL,
    has_logo TINYINT(1) NOT NULL DEFAULT 1,
    has_stripe TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_team_uniform_team_season_week (team, season_number, week)
);
