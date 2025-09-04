-- Playbook Stats Table Schema
-- This table stores aggregated statistics for offensive and defensive playbook combinations by season
-- It aggregates all team stats within a specific playbook combination for a given season

CREATE TABLE playbook_stats (
    -- Primary Key
    id INT AUTO_INCREMENT PRIMARY KEY,
    
    -- Playbook Identification
    offensive_playbook VARCHAR(255) NOT NULL,
    defensive_playbook VARCHAR(255) NOT NULL,
    season_number INT NOT NULL,
    
    -- Playbook Summary
    total_teams INT DEFAULT 0,
    total_games INT DEFAULT 0,
    
    -- Passing Stats (Playbook Totals)
    pass_attempts INT DEFAULT 0,
    pass_completions INT DEFAULT 0,
    pass_completion_percentage DOUBLE DEFAULT 0.0,
    pass_yards INT DEFAULT 0,
    pass_touchdowns INT DEFAULT 0,
    pass_interceptions INT DEFAULT 0,
    pass_successes INT DEFAULT 0,
    pass_success_percentage DOUBLE DEFAULT 0.0,
    longest_pass INT DEFAULT 0,
    
    -- Rushing Stats (Playbook Totals)
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
    
    -- Defense Stats (Playbook Totals)
    sacks_allowed INT DEFAULT 0,
    sacks_forced INT DEFAULT 0,
    interceptions_forced INT DEFAULT 0,
    fumbles_forced INT DEFAULT 0,
    fumbles_recovered INT DEFAULT 0,
    defensive_touchdowns INT DEFAULT 0,
    
    -- Special Teams Stats (Playbook Totals)
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
    
    -- Performance Metrics (Playbook Averages)
    average_offensive_diff DOUBLE DEFAULT 0.0,
    average_defensive_diff DOUBLE DEFAULT 0.0,
    average_offensive_special_teams_diff DOUBLE DEFAULT 0.0,
    average_defensive_special_teams_diff DOUBLE DEFAULT 0.0,
    average_diff DOUBLE DEFAULT 0.0,
    average_response_speed DOUBLE DEFAULT 0.0,
    
    -- Metadata
    last_modified_ts VARCHAR(255) NULL,
    
    -- Constraints
    UNIQUE KEY unique_offensive_defensive_playbook_season (offensive_playbook, defensive_playbook, season_number),
    INDEX idx_offensive_playbook (offensive_playbook),
    INDEX idx_defensive_playbook (defensive_playbook),
    INDEX idx_season_number (season_number),
    INDEX idx_offensive_defensive_playbook_season (offensive_playbook, defensive_playbook, season_number)
);

-- Example queries:

-- Get all Pro Style offensive playbook stats
-- SELECT * FROM playbook_stats WHERE offensive_playbook = 'PRO_STYLE' ORDER BY season_number DESC;

-- Get all 3-4 defensive playbook stats
-- SELECT * FROM playbook_stats WHERE defensive_playbook = 'THREE_FOUR' ORDER BY season_number DESC;

-- Get Pro Style + 3-4 playbook stats for season 11
-- SELECT * FROM playbook_stats WHERE offensive_playbook = 'PRO_STYLE' AND defensive_playbook = 'THREE_FOUR' AND season_number = 11;

-- Get all playbook stats for season 11
-- SELECT * FROM playbook_stats WHERE season_number = 11 ORDER BY offensive_playbook, defensive_playbook;

-- Get top passing yards by playbook combination and season
-- SELECT offensive_playbook, defensive_playbook, season_number, pass_yards FROM playbook_stats ORDER BY pass_yards DESC LIMIT 10;

-- Get average offensive diff by offensive playbook
-- SELECT offensive_playbook, AVG(average_offensive_diff) as avg_off_diff FROM playbook_stats GROUP BY offensive_playbook ORDER BY avg_off_diff DESC;

-- Get average defensive diff by defensive playbook
-- SELECT defensive_playbook, AVG(average_defensive_diff) as avg_def_diff FROM playbook_stats GROUP BY defensive_playbook ORDER BY avg_def_diff DESC;

-- Compare playbook combinations
-- SELECT offensive_playbook, defensive_playbook, AVG(average_diff) as avg_diff FROM playbook_stats GROUP BY offensive_playbook, defensive_playbook ORDER BY avg_diff DESC;
