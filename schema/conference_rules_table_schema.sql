-- Conference Rules Table Schema
-- This table stores conference scheduling rules including number of conference games
-- and protected rivalries for each conference

CREATE TABLE conference_rules (
    -- Primary Key
    id INT AUTO_INCREMENT PRIMARY KEY,
    
    -- Conference Identification
    conference VARCHAR(255) NOT NULL UNIQUE,
    
    -- Conference Schedule Rules
    num_conference_games INT NOT NULL DEFAULT 9,
    
    -- Protected Rivalries (stored as JSON)
    -- Format: [{"team1": "TeamA", "team2": "TeamB", "week": 5}, ...]
    protected_rivalries JSON NULL,
    
    -- Indexes
    INDEX idx_conference (conference)
);
