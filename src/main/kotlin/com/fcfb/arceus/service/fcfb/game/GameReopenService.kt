package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.domain.game.GameConditions
import com.fcfb.arceus.domain.season.SeasonStructure
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.exception.game.InvalidGameReopenException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.statistics.elo.EloService
import com.fcfb.arceus.service.fcfb.statistics.season.PostseasonSeasonStatsService
import com.fcfb.arceus.service.fcfb.statistics.season.SeasonStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import org.springframework.stereotype.Service

@Service
class GameReopenService(
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val gameService: GameService,
    private val teamService: TeamService,
    private val userService: UserService,
    private val scheduleService: ScheduleService,
    private val gameStatsService: GameStatsService,
    private val gameStatsRepository: GameStatsRepository,
    private val seasonStatsService: SeasonStatsService,
    private val postseasonSeasonStatsService: PostseasonSeasonStatsService,
    private val eloService: EloService,
) {
    private val gameConditions = GameConditions()

    fun reopenGame(gameId: Int): Game {
        val game = gameRepository.getGameById(gameId) ?: throw GameNotFoundException("No game found with ID: $gameId")
        if (game.gameStatus != GameStatus.FINAL) {
            throw InvalidGameReopenException("Game $gameId is not final")
        }
        if (game.gameType == GameType.NATIONAL_CHAMPIONSHIP) {
            throw InvalidGameReopenException("National championship games cannot be reopened")
        }
        val lastPlay = getLastPlayedPlay(gameId)
        if (!gameConditions.isTouchdownPlay(lastPlay.actualResult)) {
            throw InvalidGameReopenException("Game $gameId did not end on a touchdown")
        }
        val countsTowardStandings = game.gameType != GameType.SCRIMMAGE
        if (countsTowardStandings) {
            teamService.revertTeamWinsAndLosses(game)
            userService.revertUserWinsAndLosses(game)
            scheduleService.markGameAsUnfinished(game)
        }
        deleteEndOfGamePlay(gameId)
        restoreStateFromTouchdown(game, lastPlay)
        gameRepository.save(game)
        gameStatsService.generateGameStats(gameId)
        if (countsTowardStandings) {
            refreshSeasonStatsForGame(game)
            eloService.rebuild(apply = true)
        }
        return game
    }

    private fun deleteEndOfGamePlay(gameId: Int) {
        playRepository.getPreviousPlay(gameId)
            ?.takeIf { it.actualResult == ActualResult.END_OF_GAME }
            ?.let { playRepository.deleteById(it.playId) }
    }

    private fun getLastPlayedPlay(gameId: Int) =
        playRepository.getAllPlaysByGameId(gameId)
            .filter { it.playFinished && it.actualResult != ActualResult.END_OF_GAME }
            .maxByOrNull { it.playId }
            ?: throw InvalidGameReopenException("Game $gameId has no plays to reopen from")

    private fun restoreStateFromTouchdown(
        game: Game,
        touchdownPlay: Play,
    ) {
        game.homeScore = touchdownPlay.homeScore
        game.awayScore = touchdownPlay.awayScore
        game.possession = gameConditions.touchdownScoringSide(touchdownPlay.possession, touchdownPlay.actualResult)
        game.ballLocation = TOUCHDOWN_BALL_LOCATION
        game.currentPlayType = PlayType.PAT
        game.waitingOn = if (game.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
        game.gameStatus = if (game.quarter <= 4) GameStatus.IN_PROGRESS else GameStatus.OVERTIME
        game.gameTimer = gameService.calculateDelayOfGameTimer()
        game.gameWarning = GameWarning.NONE
    }

    private fun refreshSeasonStatsForGame(game: Game) {
        val gameStats = gameStatsRepository.findByGameId(game.gameId)
        gameStats.forEach { seasonStatsService.updateSeasonStatsForGame(it) }
        if ((game.week ?: 0) >= SeasonStructure.POSTSEASON_START_WEEK) {
            gameStats.forEach { postseasonSeasonStatsService.updateSeasonStatsForGame(it) }
        }
    }

    companion object {
        private const val TOUCHDOWN_BALL_LOCATION = 97
    }
}
