-- Remove conference column from game_stats table
-- This column is no longer needed since conference data comes from Team entity
-- Run this script after updating the GameStats model to remove the conference field

-- Check if the conference column exists before dropping it
-- This prevents errors if the column has already been removed
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'game_stats' 
     AND COLUMN_NAME = 'conference') > 0,
    'ALTER TABLE game_stats DROP COLUMN conference',
    'SELECT "Conference column does not exist, skipping drop" as message'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify the column has been removed
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN 'SUCCESS: Conference column removed from game_stats table'
        ELSE 'WARNING: Conference column still exists in game_stats table'
    END as result
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'game_stats' 
AND COLUMN_NAME = 'conference';
