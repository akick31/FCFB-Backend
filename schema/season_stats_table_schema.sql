-- SQL Schema for Season Stats Table
-- This table stores aggregated season statistics for each team
-- One row per team per season, excluding scrimmage games

CREATE TABLE season_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    team VARCHAR(255) NOT NULL,
    season_number INT NOT NULL,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    subdivision VARCHAR(50) NULL,
    
    -- Passing Stats (Season Totals)
    pass_attempts INT DEFAULT 0,
    pass_completions INT DEFAULT 0,
    pass_completion_percentage DOUBLE NULL,
    pass_yards INT DEFAULT 0,
    longest_pass INT DEFAULT 0,
    pass_touchdowns INT DEFAULT 0,
    pass_successes INT DEFAULT 0,
    pass_success_percentage DOUBLE NULL,
    
    -- Rushing Stats (Season Totals)
    rush_attempts INT DEFAULT 0,
    rush_successes INT DEFAULT 0,
    rush_success_percentage DOUBLE NULL,
    rush_yards INT DEFAULT 0,
    longest_run INT DEFAULT 0,
    rush_touchdowns INT DEFAULT 0,
    
    -- Total Offense (Season Totals)
    total_yards INT DEFAULT 0,
    average_yards_per_play DOUBLE NULL,
    first_downs INT DEFAULT 0,
    
    -- Sacks (Season Totals)
    sacks_allowed INT DEFAULT 0,
    sacks_forced INT DEFAULT 0,
    
    -- Turnovers (Season Totals)
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
    
    -- Field Goals (Season Totals)
    field_goal_made INT DEFAULT 0,
    field_goal_attempts INT DEFAULT 0,
    field_goal_percentage DOUBLE NULL,
    longest_field_goal INT DEFAULT 0,
    blocked_opponent_field_goals INT DEFAULT 0,
    field_goal_touchdown INT DEFAULT 0,
    
    -- Punting (Season Totals)
    punts_attempted INT DEFAULT 0,
    longest_punt INT DEFAULT 0,
    average_punt_length DOUBLE NULL,
    blocked_opponent_punt INT DEFAULT 0,
    punt_return_td INT DEFAULT 0,
    punt_return_td_percentage DOUBLE NULL,
    
    -- Kickoffs (Season Totals)
    number_of_kickoffs INT DEFAULT 0,
    onside_attempts INT DEFAULT 0,
    onside_success INT DEFAULT 0,
    onside_success_percentage DOUBLE NULL,
    normal_kickoff_attempts INT DEFAULT 0,
    touchbacks INT DEFAULT 0,
    touchback_percentage DOUBLE NULL,
    kick_return_td INT DEFAULT 0,
    kick_return_td_percentage DOUBLE NULL,
    
    -- Game Flow (Season Totals)
    number_of_drives INT DEFAULT 0,
    time_of_possession INT DEFAULT 0,
    
    -- Touchdowns (Season Totals)
    touchdowns INT DEFAULT 0,
    
    -- Down Conversions (Season Totals)
    third_down_conversion_success INT DEFAULT 0,
    third_down_conversion_attempts INT DEFAULT 0,
    third_down_conversion_percentage DOUBLE NULL,
    fourth_down_conversion_success INT DEFAULT 0,
    fourth_down_conversion_attempts INT DEFAULT 0,
    fourth_down_conversion_percentage DOUBLE NULL,
    
    -- Game Control (Season Totals)
    largest_lead INT DEFAULT 0,
    largest_deficit INT DEFAULT 0,
    
    -- Red Zone (Season Totals)
    red_zone_attempts INT DEFAULT 0,
    red_zone_successes INT DEFAULT 0,
    red_zone_success_percentage DOUBLE NULL,
    red_zone_percentage DOUBLE NULL,
    
    -- Special Teams (Season Totals)
    safeties_forced INT DEFAULT 0,
    safeties_committed INT DEFAULT 0,
    
    -- Performance Metrics (Season Averages)
    average_offensive_diff DOUBLE NULL,
    average_defensive_diff DOUBLE NULL,
    average_offensive_special_teams_diff DOUBLE NULL,
    average_defensive_special_teams_diff DOUBLE NULL,
    average_diff DOUBLE NULL,
    average_response_speed DOUBLE NULL,
    
    -- Additional Season Info
    last_modified_ts VARCHAR(255) NULL,
    
    -- Indexes for performance
    UNIQUE KEY unique_team_season (team, season_number),
    INDEX idx_team (team),
    INDEX idx_season_number (season_number),
    INDEX idx_wins_losses (wins DESC, losses ASC),
    INDEX idx_total_yards (total_yards DESC),
    INDEX idx_touchdowns (touchdowns DESC),
    INDEX idx_turnover_differential (turnover_differential DESC)
);

-- Comments explaining the table structure
-- team: The team name
-- season_number: Which season these stats are for
-- wins/losses: Win-loss record for the season
-- subdivision: The team's subdivision (FBS, FCS, etc.)
-- All other fields: Aggregated statistics from all non-scrimmage games for that team in that season
-- Percentages are calculated from the sum of components (e.g., total completions / total attempts)
-- Performance metrics are averages across all games for that team in that season

-- Example queries:
-- Get all season stats for a specific team
-- SELECT * FROM season_stats WHERE team = 'Alabama';

-- Get all season stats for a specific season
-- SELECT * FROM season_stats WHERE season_number = 10;

-- Get season stats for a specific team and season
-- SELECT * FROM season_stats WHERE team = 'Alabama' AND season_number = 10;

-- Get teams with most total yards in a season
-- SELECT team, season_number, total_yards FROM season_stats ORDER BY total_yards DESC LIMIT 10;

-- Get teams with best win-loss record in a season
-- SELECT team, season_number, wins, losses FROM season_stats ORDER BY wins DESC, losses ASC LIMIT 10;

-- Get teams with best turnover differential in a season
-- SELECT team, season_number, turnover_differential FROM season_stats ORDER BY turnover_differential DESC LIMIT 10;