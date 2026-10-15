package com.fcfb.arceus.domain.ranking

import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.ranking.TeamSeasonAggregate
import com.fcfb.arceus.model.team.Team

class AsrRatingsCalculator(
    private val adjustedScheduleRatingsCalculator: AdjustedScheduleRatingsCalculator,
    private val rankingFormulas: RankingFormulas,
) {
    fun calculate(
        games: List<Game>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
    ): Map<Int, Double> =
        adjustedScheduleRatingsCalculator.calculate(games, aggregates, teamsByName) {
            rankingFormulas.calculateCappedMarginOfVictory(it.cappedPointDifferential, it.gamesPlayed)
        }
}
