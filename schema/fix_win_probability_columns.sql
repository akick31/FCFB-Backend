-- Fix win probability columns to use DOUBLE instead of INT
-- This script updates the play table to use proper data types for win probability fields

-- Update win_probability column to DOUBLE
ALTER TABLE play MODIFY COLUMN win_probability DOUBLE;

-- Update win_probability_added column to DOUBLE  
ALTER TABLE play MODIFY COLUMN win_probability_added DOUBLE;

-- Add comments to clarify the columns
ALTER TABLE play MODIFY COLUMN win_probability DOUBLE COMMENT 'Win probability for the team with possession (0.0 to 1.0)';
ALTER TABLE play MODIFY COLUMN win_probability_added DOUBLE COMMENT 'Change in win probability from previous play (can be negative)';
