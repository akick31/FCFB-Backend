package com.fcfb.arceus.domain.history

import com.fcfb.arceus.dto.response.FastestGameResponse
import com.fcfb.arceus.dto.response.RankedLossResponse
import com.fcfb.arceus.dto.response.SpreadResponse
import com.fcfb.arceus.dto.response.UpsetResponse
import com.fcfb.arceus.model.game.Game
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class WeekHighlights(
    private val games: List<Game>,
) {
    fun upsets(): List<UpsetResponse> =
        games
            .filter { it.homeScore != it.awayScore && isUpset(it) }
            .map { upsetOf(it) }
            .sortedWith(compareByDescending<UpsetResponse> { it.points }.thenBy { rankOrUnranked(it.loserRank) })

    fun rankedLosses(): List<RankedLossResponse> =
        games
            .filter { it.homeScore != it.awayScore }
            .mapNotNull { game ->
                val homeWon = game.homeScore > game.awayScore
                val loserRank = ranked(if (homeWon) game.awayTeamRank else game.homeTeamRank) ?: return@mapNotNull null
                RankedLossResponse(
                    gameId = game.gameId,
                    team = if (homeWon) game.awayTeam else game.homeTeam,
                    rank = loserRank,
                    opponent = if (homeWon) game.homeTeam else game.awayTeam,
                )
            }.sortedBy { it.rank }

    fun biggestSpread(): SpreadResponse? =
        games
            .filter { (it.homeVegasSpread ?: 0.0) != 0.0 }
            .maxByOrNull { abs(it.homeVegasSpread!!) }
            ?.let { game ->
                val homeFavored = game.homeVegasSpread!! < 0
                SpreadResponse(
                    gameId = game.gameId,
                    favorite = if (homeFavored) game.homeTeam else game.awayTeam,
                    underdog = if (homeFavored) game.awayTeam else game.homeTeam,
                    points = abs(game.homeVegasSpread!!),
                    favoriteWon = (game.homeScore > game.awayScore) == homeFavored,
                )
            }

    fun closestGameId(): Int? = games.minByOrNull { margin(it) }?.gameId

    fun biggestBlowoutGameId(): Int? = games.maxByOrNull { margin(it) }?.gameId

    fun highestScoringGameId(): Int? = games.maxByOrNull { it.homeScore + it.awayScore }?.gameId

    fun fastestGame(): FastestGameResponse? =
        games
            .mapNotNull { game -> minutesPlayed(game)?.let { FastestGameResponse(game.gameId, it) } }
            .minByOrNull { it.minutes }

    fun overtimeGameIds(): List<Int> = games.filter { isOvertime(it) }.map { it.gameId }

    private fun isUpset(game: Game): Boolean {
        val homeWon = game.homeScore > game.awayScore
        val winnerSpread = if (homeWon) game.homeVegasSpread else game.awayVegasSpread
        if (winnerSpread != null && winnerSpread != 0.0) {
            return winnerSpread > 0
        }
        val winnerRank = rankOrUnranked(if (homeWon) game.homeTeamRank else game.awayTeamRank)
        val loserRank = rankOrUnranked(if (homeWon) game.awayTeamRank else game.homeTeamRank)
        return winnerRank > loserRank
    }

    private fun upsetOf(game: Game): UpsetResponse {
        val homeWon = game.homeScore > game.awayScore
        val winnerSpread = if (homeWon) game.homeVegasSpread else game.awayVegasSpread
        return UpsetResponse(
            gameId = game.gameId,
            winner = if (homeWon) game.homeTeam else game.awayTeam,
            loser = if (homeWon) game.awayTeam else game.homeTeam,
            winnerRank = ranked(if (homeWon) game.homeTeamRank else game.awayTeamRank),
            loserRank = ranked(if (homeWon) game.awayTeamRank else game.homeTeamRank),
            points = winnerSpread?.takeIf { it > 0 } ?: 0.0,
        )
    }

    private fun margin(game: Game) = abs(game.homeScore - game.awayScore)

    private fun minutesPlayed(game: Game): Long? {
        val start = game.timestamp?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: return null
        val end =
            game.lastMessageTimestamp?.let { runCatching { LocalDateTime.parse(it, MESSAGE_TIME) }.getOrNull() }
                ?: return null
        return Duration.between(start, end).toMinutes().takeIf { it > 0 }
    }

    companion object {
        private const val UNRANKED = 99
        private val MESSAGE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun ranked(rank: Int?): Int? = rank?.takeIf { it in 1..25 }

        fun isOvertime(game: Game) = game.quarter > 4

        private fun rankOrUnranked(rank: Int?) = ranked(rank) ?: UNRANKED
    }
}
