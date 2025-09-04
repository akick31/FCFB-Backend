-- SQL Schema for Records Table
-- This table stores all statistical records (both single-game and season totals)
-- Supports both HIGHEST and LOWEST records for specific performance metrics

CREATE TABLE records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_name VARCHAR(255) NOT NULL,
    record_type VARCHAR(50) NOT NULL,
    season_number INT NOT NULL,
    week INT NULL,
    game_id INT NULL,
    home_team VARCHAR(255) NULL,
    away_team VARCHAR(255) NULL,
    record_team VARCHAR(255) NOT NULL,
    coach VARCHAR(255) NULL,
    record_value DOUBLE NOT NULL,
    previous_record_value DOUBLE NULL,
    previous_record_team VARCHAR(255) NULL,
    previous_record_game_id INT NULL,
    is_tied BOOLEAN DEFAULT FALSE,
    tied_teams VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_record_name_type (record_name, record_type),
    INDEX idx_season_number (season_number),
    INDEX idx_record_team (record_team),
    INDEX idx_game_id (game_id),
    INDEX idx_record_value (record_value DESC),
    INDEX idx_season_week (season_number DESC, week DESC)
);

-- Comments explaining the table structure
-- record_name: The type of stat (e.g., 'PASS_YARDS', 'TOUCHDOWNS', 'AVERAGE_RESPONSE_SPEED', etc.)
-- record_type: One of six types:
--   - 'SINGLE_GAME': Single game performance (highest value)
--   - 'SINGLE_GAME_LOWEST': Single game performance (lowest value)
--   - 'SINGLE_SEASON': Best season performance ever (highest value)
--   - 'SINGLE_SEASON_LOWEST': Worst season performance ever (lowest value)
--   - 'GENERAL': General record that doesn't need season/game distinction (highest value)
--   - 'GENERAL_LOWEST': General record that doesn't need season/game distinction (lowest value)
-- season_number: Which season the record occurred in
-- week: Which week the game occurred in (NULL for SINGLE_SEASON records)
-- game_id: The ID of the game where the record was set (NULL for SINGLE_SEASON records)
-- home_team: Home team in the game where the record was set (NULL for SINGLE_SEASON records)
-- away_team: Away team in the game where the record was set (NULL for SINGLE_SEASON records)
-- record_team: The team that set the record
-- coach: The coach(s) who were coaching when the record was set (NULL for SINGLE_SEASON records, coach who coached most games for season records)
-- record_value: The actual value of the record
-- previous_record_value: The previous record value (for tracking record progression)
-- previous_record_team: The team that previously held the record
-- previous_record_game_id: The game ID where the previous record was set
-- is_tied: Whether this record is tied with another team
-- tied_teams: Comma-separated list of teams that are tied for this record

-- Stats that track both HIGHEST and LOWEST records:
-- - AVERAGE_OFFENSIVE_DIFF
-- - AVERAGE_DEFENSIVE_DIFF  
-- - AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF
-- - AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF
-- - AVERAGE_DIFF
-- - AVERAGE_RESPONSE_SPEED
-- - TIME_OF_POSSESSION
-- - TOTAL_YARDS
-- - AVERAGE_YARDS_PER_PLAY

-- All other stats only track HIGHEST records (GAME, SEASON, and GENERAL types)

-- Example queries:
-- Get all records for a specific stat
-- SELECT * FROM records WHERE record_name = 'TOTAL_YARDS';

-- Get highest single-game total yards record
-- SELECT * FROM records WHERE record_name = 'TOTAL_YARDS' AND record_type = 'SINGLE_GAME';

-- Get lowest single-game total yards record  
-- SELECT * FROM records WHERE record_name = 'TOTAL_YARDS' AND record_type = 'SINGLE_GAME_LOWEST';

-- Get highest season total yards record (best season ever)
-- SELECT * FROM records WHERE record_name = 'TOTAL_YARDS' AND record_type = 'SINGLE_SEASON';

-- Get lowest season total yards record (worst season ever)
-- SELECT * FROM records WHERE record_name = 'TOTAL_YARDS' AND record_type = 'SINGLE_SEASON_LOWEST';

-- Get longest field goal record (general record)
-- SELECT * FROM records WHERE record_name = 'LONGEST_FIELD_GOAL' AND record_type = 'GENERAL';

-- Get all records for a specific team
-- SELECT * FROM records WHERE record_team = 'Alabama';

-- Get all records for a specific season
-- SELECT * FROM records WHERE season_number = 5;

-- Get records broken in a specific game
-- SELECT * FROM records WHERE game_id = 123;
