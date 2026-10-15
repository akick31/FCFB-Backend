package com.fcfb.arceus.domain.ranking

import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.ranking.TeamSeasonAggregate
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.util.LinearAlgebraUtils

class ColleyRatingsCalculator {
    fun calculate(
        games: List<Game>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
    ): Map<Int, Double> {
        val teamIds = aggregates.keys.sorted()
        val n = teamIds.size
        if (n == 0) return emptyMap()
        val indexOfTeamId = teamIds.withIndex().associate { (index, teamId) -> teamId to index }

        val gamesBetween = Array(n) { IntArray(n) }
        games.forEach { game ->
            val homeTeamId = teamsByName[game.homeTeam]?.id
            val awayTeamId = teamsByName[game.awayTeam]?.id
            val homeIndex = indexOfTeamId[homeTeamId]
            val awayIndex = indexOfTeamId[awayTeamId]
            if (homeIndex != null && awayIndex != null) {
                gamesBetween[homeIndex][awayIndex] += 1
                gamesBetween[awayIndex][homeIndex] += 1
            }
        }

        val matrix =
            Array(n) { row ->
                DoubleArray(n) { col ->
                    if (row == col) {
                        2.0 + aggregates.getValue(teamIds[row]).gamesPlayed
                    } else {
                        -gamesBetween[row][col].toDouble()
                    }
                }
            }
        val b =
            DoubleArray(n) { row ->
                val aggregate = aggregates.getValue(teamIds[row])
                1.0 + (aggregate.wins - aggregate.losses) / 2.0
            }

        val ratings = LinearAlgebraUtils().solve(matrix, b)
        return teamIds.indices.associate { index -> teamIds[index] to ratings[index] }
    }
}
