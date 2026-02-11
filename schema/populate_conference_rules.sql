-- Populate Conference Rules Table with Default Values
-- This script inserts default conference rules (9 conference games) for all conferences
-- Run this after creating the conference_rules table

INSERT INTO conference_rules (conference, num_conference_games, protected_rivalries)
VALUES
    ('ACC', 9, NULL),
    ('AMERICAN', 9, NULL),
    ('BIG_12', 9, NULL),
    ('BIG_TEN', 9, NULL),
    ('FAKE_TEAM', 9, NULL),
    ('FBS_INDEPENDENT', 9, NULL),
    ('MAC', 9, NULL),
    ('MOUNTAIN_WEST', 9, NULL),
    ('PAC_12', 9, NULL),
    ('SEC', 9, NULL),
    ('SUN_BELT', 9, NULL),
    ('MISSOURI_VALLEY', 9, NULL),
    ('COLONIAL', 9, NULL),
    ('NEC', 9, NULL)
ON DUPLICATE KEY UPDATE
    num_conference_games = VALUES(num_conference_games);
