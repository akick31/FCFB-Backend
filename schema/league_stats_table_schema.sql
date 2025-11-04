-- League Stats Table Schema
-- This table stores aggregated statistics for entire leagues (subdivisions) by season
-- It aggregates all team stats within a subdivision for a given season

CREATE TABLE league_stats (
    -- Primary Key
    id INT AUTO_INCREMENT PRIMARY KEY,
    
    -- League Identification
    subdivision VARCHAR(255) NOT NULL,
    season_number INT NOT NULL,
    
    -- League Summary
    total_teams INT DEFAULT 0,
    total_games INT DEFAULT 0,
    
    -- Passing Stats (League Totals)
    pass_attempts INT DEFAULT 0,
    pass_completions INT DEFAULT 0,
    pass_completion_percentage DOUBLE DEFAULT 0.0,
    pass_yards INT DEFAULT 0,
    pass_touchdowns INT DEFAULT 0,
    pass_interceptions INT DEFAULT 0,
    pass_successes INT DEFAULT 0,
    pass_success_percentage DOUBLE DEFAULT 0.0,
    longest_pass INT DEFAULT 0,
    
    -- Rushing Stats (League Totals)
    rush_attempts INT DEFAULT 0,
    rush_successes INT DEFAULT 0,
    rush_success_percentage DOUBLE DEFAULT 0.0,
    rush_yards INT DEFAULT 0,
    rush_touchdowns INT DEFAULT 0,
    longest_run INT DEFAULT 0,
    
    -- Total Offense
    total_yards INT DEFAULT 0,
    average_yards_per_play DOUBLE DEFAULT 0.0,
    first_downs INT DEFAULT 0,
    
    -- Defense Stats (League Totals)
    sacks_allowed INT DEFAULT 0,
    sacks_forced INT DEFAULT 0,
    interceptions_forced INT DEFAULT 0,
    fumbles_forced INT DEFAULT 0,
    fumbles_recovered INT DEFAULT 0,
    defensive_touchdowns INT DEFAULT 0,
    
    -- Special Teams Stats (League Totals)
    field_goals_attempted INT DEFAULT 0,
    field_goals_made INT DEFAULT 0,
    field_goal_percentage DOUBLE DEFAULT 0.0,
    longest_field_goal INT DEFAULT 0,
    extra_points_attempted INT DEFAULT 0,
    extra_points_made INT DEFAULT 0,
    extra_point_percentage DOUBLE DEFAULT 0.0,
    punts INT DEFAULT 0,
    punt_yards INT DEFAULT 0,
    longest_punt INT DEFAULT 0,
    kickoff_returns INT DEFAULT 0,
    kickoff_return_yards INT DEFAULT 0,
    kickoff_return_touchdowns INT DEFAULT 0,
    punt_returns INT DEFAULT 0,
    punt_return_yards INT DEFAULT 0,
    punt_return_touchdowns INT DEFAULT 0,
    
    -- Performance Metrics (League Averages)
    average_offensive_diff DOUBLE DEFAULT 0.0,
    average_defensive_diff DOUBLE DEFAULT 0.0,
    average_offensive_special_teams_diff DOUBLE DEFAULT 0.0,
    average_defensive_special_teams_diff DOUBLE DEFAULT 0.0,
    average_diff DOUBLE DEFAULT 0.0,
    average_response_speed DOUBLE DEFAULT 0.0,
    
    -- Metadata
    last_modified_ts VARCHAR(255) NULL,
    
    -- Constraints
    UNIQUE KEY unique_subdivision_season (subdivision, season_number),
    INDEX idx_subdivision (subdivision),
    INDEX idx_season_number (season_number),
    INDEX idx_subdivision_season (subdivision, season_number)
);

-- Example queries:

-- Get all FBS league stats
-- SELECT * FROM league_stats WHERE subdivision = 'FBS' ORDER BY season_number DESC;

-- Get FBS league stats for season 11
-- SELECT * FROM league_stats WHERE subdivision = 'FBS' AND season_number = 11;

-- Get all league stats for season 11
-- SELECT * FROM league_stats WHERE season_number = 11 ORDER BY subdivision;

-- Get top passing yards by league and season
-- SELECT subdivision, season_number, pass_yards FROM league_stats ORDER BY pass_yards DESC LIMIT 10;

-- Get average offensive diff by subdivision
-- SELECT subdivision, AVG(average_offensive_diff) as avg_off_diff FROM league_stats GROUP BY subdivision ORDER BY avg_off_diff DESC;