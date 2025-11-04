-- Conference Stats Table Schema
-- This table stores aggregated statistics for conferences within subdivisions by season
-- It aggregates all team statistics within a conference for a given season

CREATE TABLE conference_stats (
    -- Primary Key
    id INT AUTO_INCREMENT PRIMARY KEY,
    
    -- Conference Identification
    subdivision VARCHAR(255) NOT NULL,
    conference VARCHAR(255) NOT NULL,
    season_number INT NOT NULL,
    
    -- Conference Summary
    total_teams INT DEFAULT 0,
    total_games INT DEFAULT 0,
    
    -- Passing Stats (League Totals)
    pass_attempts INT DEFAULT 0,
    pass_completions INT DEFAULT 0,
    pass_completion_percentage DOUBLE NULL,
    pass_yards INT DEFAULT 0,
    longest_pass INT DEFAULT 0,
    pass_touchdowns INT DEFAULT 0,
    pass_successes INT DEFAULT 0,
    pass_success_percentage DOUBLE NULL,
    
    -- Rushing Stats (League Totals)
    rush_attempts INT DEFAULT 0,
    rush_successes INT DEFAULT 0,
    rush_success_percentage DOUBLE NULL,
    rush_yards INT DEFAULT 0,
    longest_run INT DEFAULT 0,
    rush_touchdowns INT DEFAULT 0,
    
    -- Total Offense (League Totals)
    total_yards INT DEFAULT 0,
    average_yards_per_play DOUBLE NULL,
    first_downs INT DEFAULT 0,
    
    -- Sacks (League Totals)
    sacks_allowed INT DEFAULT 0,
    sacks_forced INT DEFAULT 0,
    
    -- Turnovers (League Totals)
    interceptions_lost INT DEFAULT 0,
    interceptions_forced INT DEFAULT 0,
    fumbles_lost INT DEFAULT 0,
    fumbles_forced INT DEFAULT 0,
    turnovers_lost INT DEFAULT 0,
    turnovers_forced INT DEFAULT 0,
    turnover_differential INT DEFAULT 0,
    turnover_touchdowns_lost INT DEFAULT 0,
    turnover_touchdowns_forced INT DEFAULT 0,
    pick_sixes_thrown INT DEFAULT 0,
    pick_sixes_forced INT DEFAULT 0,
    fumble_return_tds_committed INT DEFAULT 0,
    fumble_return_tds_forced INT DEFAULT 0,
    
    -- Field Goals (League Totals)
    field_goal_made INT DEFAULT 0,
    field_goal_attempts INT DEFAULT 0,
    field_goal_percentage DOUBLE NULL,
    longest_field_goal INT DEFAULT 0,
    blocked_opponent_field_goals INT DEFAULT 0,
    field_goal_touchdown INT DEFAULT 0,
    
    -- Punting (League Totals)
    punts_attempted INT DEFAULT 0,
    longest_punt INT DEFAULT 0,
    average_punt_length DOUBLE NULL,
    blocked_opponent_punt INT DEFAULT 0,
    punt_return_td INT DEFAULT 0,
    punt_return_td_percentage DOUBLE NULL,
    
    -- Kickoffs (League Totals)
    number_of_kickoffs INT DEFAULT 0,
    onside_attempts INT DEFAULT 0,
    onside_success INT DEFAULT 0,
    onside_success_percentage DOUBLE NULL,
    normal_kickoff_attempts INT DEFAULT 0,
    touchbacks INT DEFAULT 0,
    touchback_percentage DOUBLE NULL,
    kick_return_td INT DEFAULT 0,
    kick_return_td_percentage DOUBLE NULL,
    
    -- Game Flow (League Totals)
    number_of_drives INT DEFAULT 0,
    time_of_possession INT DEFAULT 0,
    
    -- Touchdowns (League Totals)
    touchdowns INT DEFAULT 0,
    
    -- Down Conversions (League Totals)
    third_down_conversion_success INT DEFAULT 0,
    third_down_conversion_attempts INT DEFAULT 0,
    third_down_conversion_percentage DOUBLE NULL,
    fourth_down_conversion_success INT DEFAULT 0,
    fourth_down_conversion_attempts INT DEFAULT 0,
    fourth_down_conversion_percentage DOUBLE NULL,
    
    -- Game Control (League Totals)
    largest_lead INT DEFAULT 0,
    largest_deficit INT DEFAULT 0,
    
    -- Red Zone (League Totals)
    red_zone_attempts INT DEFAULT 0,
    red_zone_successes INT DEFAULT 0,
    red_zone_success_percentage DOUBLE NULL,
    red_zone_percentage DOUBLE NULL,
    
    -- Special Teams (League Totals)
    safeties_forced INT DEFAULT 0,
    safeties_committed INT DEFAULT 0,
    
    -- Performance Metrics (League Averages - averages of team averages)
    average_offensive_diff DOUBLE NULL,
    average_defensive_diff DOUBLE NULL,
    average_offensive_special_teams_diff DOUBLE NULL,
    average_defensive_special_teams_diff DOUBLE NULL,
    average_diff DOUBLE NULL,
    average_response_speed DOUBLE NULL,
    
    -- Metadata
    last_modified_ts VARCHAR(255) NULL,
    
    -- Constraints
    UNIQUE KEY unique_subdivision_conference_season (subdivision, conference, season_number),
    INDEX idx_subdivision (subdivision),
    INDEX idx_conference (conference),
    INDEX idx_season_number (season_number),
    INDEX idx_subdivision_conference_season (subdivision, conference, season_number)
);

-- Example queries:

-- Get all FBS conference stats
-- SELECT * FROM conference_stats WHERE subdivision = 'FBS' ORDER BY season_number DESC;

-- Get SEC conference stats for season 11
-- SELECT * FROM conference_stats WHERE conference = 'SEC' AND season_number = 11;

-- Get FBS conference stats for season 11
-- SELECT * FROM conference_stats WHERE subdivision = 'FBS' AND season_number = 11;

-- Get all conference stats for season 11
-- SELECT * FROM conference_stats WHERE season_number = 11 ORDER BY subdivision, conference;

-- Get top passing yards by conference and season
-- SELECT subdivision, conference, season_number, pass_yards FROM conference_stats ORDER BY pass_yards DESC LIMIT 10;

-- Get average offensive diff by subdivision
-- SELECT subdivision, AVG(average_offensive_diff) as avg_off_diff FROM conference_stats GROUP BY subdivision ORDER BY avg_off_diff DESC;

-- Get average offensive diff by conference
-- SELECT conference, AVG(average_offensive_diff) as avg_off_diff FROM conference_stats GROUP BY conference ORDER BY avg_off_diff DESC;
