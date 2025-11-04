-- Fix win probability columns to support decimal values
-- The current FLOAT(23,0) only allows integers, we need to change to DOUBLE or FLOAT with decimal places

-- Fix play table win probability columns
ALTER TABLE play 
MODIFY COLUMN win_probability DOUBLE NULL COMMENT 'Win probability for the possessing team (0.0 to 1.0)',
MODIFY COLUMN win_probability_added DOUBLE NULL COMMENT 'Change in win probability from previous play';

-- Fix game table win probability column (if it has the same issue)
ALTER TABLE game 
MODIFY COLUMN win_probability DOUBLE NULL COMMENT 'Current win probability for the game';

-- Verify the changes
-- You can run these queries to check the column types:
-- DESCRIBE play;
-- DESCRIBE game;
