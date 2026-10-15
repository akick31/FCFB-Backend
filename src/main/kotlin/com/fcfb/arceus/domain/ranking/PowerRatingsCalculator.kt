package com.fcfb.arceus.domain.ranking

import com.fcfb.arceus.model.ranking.TeamDiffAggregate
import com.fcfb.arceus.model.ranking.TeamSeasonAggregate
import com.fcfb.arceus.util.ranking.RatingNormalizer

class PowerRatingsCalculator(
    private val ratingNormalizer: RatingNormalizer,
    private val rankingFormulas: RankingFormulas,
) {
    fun calculate(
        aggregates: Map<Int, TeamSeasonAggregate>,
        diffAggregates: Map<Int, TeamDiffAggregate>,
    ): Map<Int, Double> {
        val teamIds = aggregates.keys.intersect(diffAggregates.keys)
        if (teamIds.isEmpty()) return emptyMap()

        val winPct =
            ratingNormalizer.normalize(
                teamIds.associateWith { teamId ->
                    val aggregate = aggregates.getValue(teamId)
                    if (aggregate.gamesPlayed == 0) 0.0 else aggregate.wins.toDouble() / aggregate.gamesPlayed
                },
            )
        val equivalentWins =
            ratingNormalizer.normalize(
                teamIds.associateWith { teamId ->
                    val aggregate = aggregates.getValue(teamId)
                    rankingFormulas.calculateEquivalentWins(aggregate.pointsFor, aggregate.pointsAgainst, aggregate.gamesPlayed)
                },
            )

        val offensiveDiff = ratingNormalizer.normalize(teamIds.associateWith { -diffAggregates.getValue(it).averageOffensiveDiff })
        val defensiveDiff = ratingNormalizer.normalize(teamIds.associateWith { diffAggregates.getValue(it).averageDefensiveDiff })
        val offensiveSpecialTeamsDiff =
            ratingNormalizer.normalize(teamIds.associateWith { -diffAggregates.getValue(it).averageOffensiveSpecialTeamsDiff })
        val defensiveSpecialTeamsDiff =
            ratingNormalizer.normalize(teamIds.associateWith { diffAggregates.getValue(it).averageDefensiveSpecialTeamsDiff })

        return teamIds.associateWith { teamId ->
            val base = BASE_EQUIVALENT_WINS_WEIGHT * equivalentWins.getValue(teamId) + BASE_WIN_PCT_WEIGHT * winPct.getValue(teamId)
            val normalPlayDiff = (offensiveDiff.getValue(teamId) + defensiveDiff.getValue(teamId)) / 2
            val specialTeamsDiff = (offensiveSpecialTeamsDiff.getValue(teamId) + defensiveSpecialTeamsDiff.getValue(teamId)) / 2
            val differential = NORMAL_PLAY_DIFF_WEIGHT * normalPlayDiff + SPECIAL_TEAMS_DIFF_WEIGHT * specialTeamsDiff
            POWER_RATING_BASE_WEIGHT * base + POWER_RATING_DIFFERENTIAL_WEIGHT * differential
        }
    }

    companion object {
        const val BASE_EQUIVALENT_WINS_WEIGHT = 0.65
        const val BASE_WIN_PCT_WEIGHT = 0.35
        const val NORMAL_PLAY_DIFF_WEIGHT = 0.95
        const val SPECIAL_TEAMS_DIFF_WEIGHT = 0.05
        const val POWER_RATING_BASE_WEIGHT = 0.8
        const val POWER_RATING_DIFFERENTIAL_WEIGHT = 0.2
    }
}
