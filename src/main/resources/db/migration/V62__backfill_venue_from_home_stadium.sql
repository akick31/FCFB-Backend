-- Postseason venue tracking is new as of this season; old postseason games have
-- `neutral_site` left false regardless of where they were actually played, so
-- postseason games (week >= SeasonStructure.POSTSEASON_START_WEEK, i.e. 14) are
-- excluded here rather than relying on that flag. Non-postseason games still
-- respect an explicit neutral_site flag.
UPDATE `game` g
JOIN `team` t ON t.name = g.home_team
SET g.venue = t.stadium
WHERE g.venue IS NULL
  AND g.neutral_site = 0
  AND (g.week IS NULL OR g.week < 14)
  AND t.stadium IS NOT NULL;

UPDATE `game_presentation` gp
JOIN `game` g ON g.game_id = gp.game_id
SET gp.venue = g.venue
WHERE gp.venue IS NULL
  AND g.venue IS NOT NULL;
