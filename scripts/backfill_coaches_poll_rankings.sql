-- One-off backfill of the `ranking` table (COACHES_POLL only) from the game table.
-- Run ONCE after V4/V5 create the empty `ranking` table (rank + record columns).
--
-- Notes / caveats:
--   * The game table is the only historical source and is imperfect (stale/duplicate
--     ranks in a week). This reconstruction renumbers each week's ranked teams
--     contiguously (ROW_NUMBER by recorded rank), so weeks are gap-free but a given
--     week's numbers may differ slightly from the poll actually posted that week.
--   * Regular-season weeks 1-13 are kept as-is. Weeks > 13 (conference championships +
--     postseason) collapse into a single bucket week=14, using each team's EARLIEST
--     postseason game (rank + record entering the postseason -- no new polls after CCGs).
--   * `wins`/`losses` are the record the team carried INTO that week (the game table
--     stores the pre-game record), matching what an upload captures at week start.
--   * COACHES_POLL only. The game table has no committee history, so PLAYOFF_COMMITTEE
--     is not backfilled -- upload those weeks via the admin dashboard.

INSERT INTO `ranking` (`season`, `week`, `poll_type`, `poll_rank`, `team_id`, `wins`, `losses`)
SELECT
    season,
    week_bucket,
    'COACHES_POLL',
    ROW_NUMBER() OVER (PARTITION BY season, week_bucket ORDER BY recorded_rank ASC, team_id ASC),
    team_id,
    wins,
    losses
FROM (
    SELECT
        season,
        week_bucket,
        team_id,
        recorded_rank,
        wins,
        losses,
        ROW_NUMBER() OVER (PARTITION BY season, week_bucket, team_id ORDER BY week ASC) AS pick
    FROM (
        SELECT
            g.season AS season,
            CASE WHEN g.week > 13 THEN 14 ELSE g.week END AS week_bucket,
            g.week AS week,
            t.id AS team_id,
            g.home_team_rank AS recorded_rank,
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
            g.away_team_rank,
            g.away_wins,
            g.away_losses
        FROM game g
        JOIN team t ON t.name = g.away_team
        WHERE g.season IS NOT NULL AND g.week IS NOT NULL AND g.away_team_rank BETWEEN 1 AND 25
    ) ranked_games
) picked
WHERE pick = 1;
