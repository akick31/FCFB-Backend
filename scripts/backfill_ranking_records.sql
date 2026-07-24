-- Populate `wins`/`losses` on EXISTING backfilled COACHES_POLL ranking rows.
-- Run this ONCE after V5 adds the record columns, IF you already ran the original
-- rank-only backfill (it leaves wins/losses NULL). It only sets the record; it does
-- not touch rank. For a fresh table, use backfill_coaches_poll_rankings.sql instead.
--
-- Record source: the pre-game record the team carried INTO that week (see game table).
-- Postseason weeks 14-18 collapse to week=14 using each team's EARLIEST postseason game.

UPDATE `ranking` r
JOIN (
    SELECT season, week_bucket, team_id, wins, losses
    FROM (
        SELECT
            season,
            week_bucket,
            team_id,
            wins,
            losses,
            ROW_NUMBER() OVER (PARTITION BY season, week_bucket, team_id ORDER BY week ASC) AS pick
        FROM (
            SELECT
                g.season AS season,
                CASE WHEN g.week > 13 THEN 14 ELSE g.week END AS week_bucket,
                g.week AS week,
                t.id AS team_id,
                g.home_wins AS wins,
                g.home_losses AS losses
            FROM game g
            JOIN team t ON t.name = g.home_team
            WHERE g.season IS NOT NULL AND g.week IS NOT NULL AND g.home_team_rank BETWEEN 1 AND 25
            UNION ALL
            SELECT
                g.season,
                CASE WHEN g.week > 13 THEN 14 ELSE g.week END,
                g.week,
                t.id,
                g.away_wins,
                g.away_losses
            FROM game g
            JOIN team t ON t.name = g.away_team
            WHERE g.season IS NOT NULL AND g.week IS NOT NULL AND g.away_team_rank BETWEEN 1 AND 25
        ) ranked_games
    ) picked
    WHERE pick = 1
) rec ON rec.season = r.season AND rec.week_bucket = r.week AND rec.team_id = r.team_id
SET r.wins = rec.wins, r.losses = rec.losses
WHERE r.poll_type = 'COACHES_POLL';
