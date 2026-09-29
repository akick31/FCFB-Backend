package com.fcfb.arceus.service.fcfb.elo

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game

object EloReplay {
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
