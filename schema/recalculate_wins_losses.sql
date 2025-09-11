-- Reset all totals first
UPDATE team
SET
    overall_wins = 0,
    overall_losses = 0,
    overall_conference_wins = 0,
    overall_conference_losses = 0;

-- Aggregate all games at once
UPDATE team t
    JOIN (
        SELECT
            t2.name AS team_name,
            SUM(
                    (g.home_team = t2.name AND g.home_score > g.away_score) OR
                    (g.away_team = t2.name AND g.away_score > g.home_score)
            ) AS wins,
            SUM(
                    (g.home_team = t2.name AND g.home_score < g.away_score) OR
                    (g.away_team = t2.name AND g.away_score < g.home_score)
            ) AS losses,
            SUM(
                    (g.game_type = 'CONFERENCE_GAME' OR g.game_type = 'CONFERENCE_CHAMPIONSHIP') AND (
                        (g.home_team = t2.name AND g.home_score > g.away_score) OR
                        (g.away_team = t2.name AND g.away_score > g.home_score)
                        )
            ) AS conf_wins,
            SUM(
                    (g.game_type = 'CONFERENCE_GAME' OR g.game_type = 'CONFERENCE_CHAMPIONSHIP') AND (
                        (g.home_team = t2.name AND g.home_score < g.away_score) OR
                        (g.away_team = t2.name AND g.away_score < g.home_score)
                        )
            ) AS conf_losses
        FROM team t2
                 LEFT JOIN game g ON g.home_team = t2.name OR g.away_team = t2.name
        WHERE g.game_type != 'SCRIMMAGE'
        GROUP BY t2.name
    ) AS stats
    ON t.name = stats.team_name
SET
    t.overall_wins = stats.wins,
    t.overall_losses = stats.losses,
    t.overall_conference_wins = stats.conf_wins,
    t.overall_conference_losses = stats.conf_losses;