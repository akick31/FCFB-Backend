package com.fcfb.arceus.service.fcfb.history

import com.fcfb.arceus.domain.history.WeekHighlights
import com.fcfb.arceus.dto.response.HistoryGameResponse
import com.fcfb.arceus.dto.response.WeekHighlightsResponse
import com.fcfb.arceus.dto.response.WeekHistoryResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.service.fcfb.play.WinProbabilitySwingService
import org.springframework.stereotype.Service

@Service
class WeekHistoryService(
    private val gameRepository: GameRepository,
    private val winProbabilitySwingService: WinProbabilitySwingService,
) {
    fun finishedWeeks(season: Int): List<Int> = gameRepository.getFinishedWeeksBySeason(season)

    fun week(
        season: Int,
        week: Int,
    ): WeekHistoryResponse {
        val games = gameRepository.getGamesBySeasonAndWeek(season, week).filter { it.gameStatus == GameStatus.FINAL }
        return WeekHistoryResponse(
            season = season,
            week = week,
            games = games.map { historyGame(it) },
            highlights = highlights(WeekHighlights(games), season, week),
        )
    }

    private fun highlights(
        weekHighlights: WeekHighlights,
        season: Int,
        week: Int,
    ) = WeekHighlightsResponse(
        upsets = weekHighlights.upsets(),
        rankedLosses = weekHighlights.rankedLosses(),
        biggestSpread = weekHighlights.biggestSpread(),
        closestGameId = weekHighlights.closestGameId(),
        biggestBlowoutGameId = weekHighlights.biggestBlowoutGameId(),
        highestScoringGameId = weekHighlights.highestScoringGameId(),
        fastestGame = weekHighlights.fastestGame(),
        overtimeGameIds = weekHighlights.overtimeGameIds(),
        biggestSwing = winProbabilitySwingService.biggestSwing(season, week),
    )

    private fun historyGame(game: Game) =
        HistoryGameResponse(
            gameId = game.gameId,
            homeTeam = game.homeTeam,
            awayTeam = game.awayTeam,
            homeScore = game.homeScore,
            awayScore = game.awayScore,
            homeRank = WeekHighlights.ranked(game.homeTeamRank),
            awayRank = WeekHighlights.ranked(game.awayTeamRank),
            homeSpread = game.homeVegasSpread,
            gameType = game.gameType?.name,
            postseasonName = game.postseasonGameName,
            overtime = WeekHighlights.isOvertime(game),
        )
}
