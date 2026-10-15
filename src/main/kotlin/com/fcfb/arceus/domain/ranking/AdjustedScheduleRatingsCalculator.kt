package com.fcfb.arceus.domain.ranking

import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.ranking.TeamSeasonAggregate
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.util.LinearAlgebraUtils

class AdjustedScheduleRatingsCalculator {
    fun calculate(
        games: List<Game>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
        perGameValue: (TeamSeasonAggregate) -> Double,
    ): Map<Int, Double> {
        val teamIds = aggregates.keys.sorted()
        val n = teamIds.size
        if (n == 0) return emptyMap()
        val indexOfTeamId = teamIds.withIndex().associate { (index, teamId) -> teamId to index }

        val gamesBetween = Array(n) { IntArray(n) }
        games.forEach { game ->
            val homeIndex = indexOfTeamId[teamsByName[game.homeTeam]?.id]
            val awayIndex = indexOfTeamId[teamsByName[game.awayTeam]?.id]
            if (homeIndex != null && awayIndex != null) {
                gamesBetween[homeIndex][awayIndex] += 1
                gamesBetween[awayIndex][homeIndex] += 1
            }
        }

        val matrix =
            Array(n) { row ->
                val gamesPlayed = aggregates.getValue(teamIds[row]).gamesPlayed
                DoubleArray(n) { col ->
                    if (row == col) 1.0 + REGULARIZATION else -(gamesBetween[row][col].toDouble() / gamesPlayed)
                }
            }
        val b = DoubleArray(n) { row -> perGameValue(aggregates.getValue(teamIds[row])) }

        val ratings = LinearAlgebraUtils().solve(matrix, b)
        return teamIds.indices.associate { index -> teamIds[index] to ratings[index] }
    }

    companion object {
        private const val REGULARIZATION = 0.1
    }
}
