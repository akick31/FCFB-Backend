package com.fcfb.arceus.service.fcfb.elo

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import kotlin.math.pow
import kotlin.math.roundToInt

object EloCalculator {
    const val BASE_ELO = 1500.0
    const val K_FACTOR = 32.0
    private const val POINTS_PER_HUNDRED_ELO = 3.0
    private const val HOME_FIELD_ADVANTAGE = 2.5

    fun expectedScore(
        rating: Double,
        opponentRating: Double,
    ): Double = 1.0 / (1.0 + 10.0.pow((opponentRating - rating) / 400.0))

    fun updatedRatings(
        homeElo: Double,
        awayElo: Double,
        homeWon: Boolean,
    ): Pair<Double, Double> {
        val expectedHome = expectedScore(homeElo, awayElo)
        val actualHome = if (homeWon) 1.0 else 0.0
        val delta = K_FACTOR * (actualHome - expectedHome)
        return (homeElo + delta) to (awayElo - delta)
    }

    fun homeSpread(
        homeElo: Double,
        awayElo: Double,
    ): Double {
        val spread = ((homeElo - awayElo) / 100.0) * POINTS_PER_HUNDRED_ELO + HOME_FIELD_ADVANTAGE
        return -((spread * 2).roundToInt() / 2.0)
    }

    fun replay(games: List<Game>): EloReplayResult {
        val ratings = HashMap<String, Double>()
        val preGameElo = HashMap<Pair<Int, String>, Double>()
        val chronological = games.sortedWith(compareBy({ it.season ?: 0 }, { it.week ?: 0 }, { it.gameId }))
        for (game in chronological) {
            val homeElo = ratings.getOrPut(game.homeTeam) { EloCalculator.BASE_ELO }
            val awayElo = ratings.getOrPut(game.awayTeam) { EloCalculator.BASE_ELO }
            preGameElo[game.gameId to game.homeTeam] = homeElo
            preGameElo[game.gameId to game.awayTeam] = awayElo
            if (game.gameStatus == GameStatus.FINAL && game.gameType != GameType.SCRIMMAGE) {
                val (newHome, newAway) = EloCalculator.updatedRatings(homeElo, awayElo, game.homeScore > game.awayScore)
                ratings[game.homeTeam] = newHome
                ratings[game.awayTeam] = newAway
            }
        }
        return EloReplayResult(preGameElo, ratings)
    }
}
