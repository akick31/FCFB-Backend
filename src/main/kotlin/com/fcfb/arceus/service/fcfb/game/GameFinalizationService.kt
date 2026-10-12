package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.appearance.GameFieldService
import com.fcfb.arceus.service.fcfb.probability.WinProbabilityService
import com.fcfb.arceus.service.fcfb.record.RecordService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import com.fcfb.arceus.service.fcfb.season.SeasonService
import com.fcfb.arceus.service.fcfb.statistics.CoachStatsService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.statistics.PostseasonSeasonStatsService
import com.fcfb.arceus.service.fcfb.statistics.SeasonStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class GameFinalizationService(
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val teamService: TeamService,
    private val discordService: DiscordService,
    private val userService: UserService,
    private val gameStatsService: GameStatsService,
    private val seasonService: SeasonService,
    private val scheduleService: ScheduleService,
    private val recordService: RecordService,
    private val seasonStatsService: SeasonStatsService,
    private val postseasonSeasonStatsService: PostseasonSeasonStatsService,
    private val coachStatsService: CoachStatsService,
    private val winProbabilityService: WinProbabilityService,
    private val gameFieldService: GameFieldService,
    private val transactionManager: PlatformTransactionManager,
) {
    private val finalizationTransaction = TransactionTemplate(transactionManager)

    fun endAllGames(): List<Game> {
        val gamesToEnd = getAllOngoingGames()
        val endedGames = mutableListOf<Game>()
        for (game in gamesToEnd) {
            endedGames.add(finalizeGameInTransaction(game) { endGame(game) })
        }
        return endedGames
    }

    fun endDOGOutGame(
        game: Game,
        delayOfGameInstances: Pair<Int, Int>,
    ): Game =
        finalizeGameInTransaction(game) {
            if (delayOfGameInstances.first >= 3) {
                while (game.homeScore >= game.awayScore) {
                    game.awayScore += 8
                }
            } else if (delayOfGameInstances.second >= 3) {
                while (game.awayScore >= game.homeScore) {
                    game.homeScore += 8
                }
            }
            val updatedGame = gameRepository.save(game)
            endGame(updatedGame)
            game
        }

    fun endSingleGameByChannelId(channelId: ULong): Game {
        val game = getGameByPlatformId(channelId)
        return finalizeGameInTransaction(game) { endGame(game) }
    }

    fun endSingleGameByGameId(gameId: Int): Game {
        val game = getGameById(gameId)
        return finalizeGameInTransaction(game) { endGame(game) }
    }

    private fun finalizeGameInTransaction(
        game: Game,
        operation: () -> Game,
    ): Game =
        finalizationTransaction.execute {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCompletion(status: Int) {
                        if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                            discordService.notifyFinalizationRollback(game)
                        }
                    }
                },
            )
            operation()
        }!!

    private fun endGame(
        game: Game,
        wasAlreadyFinal: Boolean = game.gameStatus == GameStatus.FINAL,
    ): Game {
        try {
            game.gameStatus = GameStatus.FINAL
            gameFieldService.snapshotIfAbsent(game)
            appendEndOfGamePlay(game)
            if (game.gameType != GameType.SCRIMMAGE && !wasAlreadyFinal) {
                teamService.updateTeamWinsAndLosses(game)
                userService.updateUserWinsAndLosses(game)
                (game.homeCoachDiscordIds.orEmpty() + game.awayCoachDiscordIds.orEmpty()).distinct().forEach { discordId ->
                    userService.findUserByDiscordId(discordId)?.let { coachStatsService.regenerateForCoach(it) }
                }

                val homeTeam = teamService.getTeamByName(game.homeTeam)
                val awayTeam = teamService.getTeamByName(game.awayTeam)
                winProbabilityService.updateEloRatings(game, homeTeam, awayTeam)
                teamService.updateCurrentElo(homeTeam)
                teamService.updateCurrentElo(awayTeam)

                val homeUsers = game.homeCoachDiscordIds!!.map { userService.getUserByDiscordId(it) }
                val awayUsers = game.awayCoachDiscordIds!!.map { userService.getUserByDiscordId(it) }
                for (user in homeUsers + awayUsers) {
                    val responseTime =
                        playRepository.getUserAverageResponseTime(
                            user.discordId
                                ?: throw Exception(
                                    "User does not have a discord id, " +
                                        "could not get average response time for user ${user.username}",
                                ),
                            seasonService.getCurrentSeason().seasonNumber,
                        ) ?: throw Exception("Could not get average response time for user ${user.username}")
                    userService.updateUserAverageResponseTime(user.id, responseTime)
                }

                scheduleService.markGameAsFinished(game)
            }
            if (game.gameType == GameType.NATIONAL_CHAMPIONSHIP) {
                seasonService.endSeason(game)
            }
            gameRepository.save(game)

            val homeStats = gameStatsService.getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
            val awayStats = gameStatsService.getGameStatsByIdAndTeam(game.gameId, game.awayTeam)
            homeStats.gameStatus = GameStatus.FINAL
            awayStats.gameStatus = GameStatus.FINAL
            gameStatsService.saveGameStats(homeStats)
            gameStatsService.saveGameStats(awayStats)

            recordService.checkAndUpdateRecordsForGame(game)

            if (game.gameType != GameType.SCRIMMAGE) {
                seasonStatsService.updateSeasonStatsForGame(homeStats)
                seasonStatsService.updateSeasonStatsForGame(awayStats)
            }

            if (game.gameType != GameType.SCRIMMAGE && (game.week ?: 0) >= POSTSEASON_START_WEEK) {
                postseasonSeasonStatsService.updateSeasonStatsForGame(homeStats)
                postseasonSeasonStatsService.updateSeasonStatsForGame(awayStats)
            }

            Logger.info(
                "Game ended.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Game Type: ${game.gameType}\n" +
                    "Home team: ${game.homeTeam}\n" +
                    "Away team: ${game.awayTeam}",
            )
            return game
        } catch (e: Exception) {
            Logger.error(
                "Error ending game.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Error Message: ${e.message ?: "Unknown error"}",
            )
            throw e
        }
    }

    private fun appendEndOfGamePlay(game: Game) {
        val lastPlay = playRepository.getPreviousPlay(game.gameId) ?: return
        if (lastPlay.actualResult == ActualResult.END_OF_GAME || game.homeScore == game.awayScore) {
            return
        }
        val homeWon = game.homeScore > game.awayScore
        val terminalWinProbability = if ((lastPlay.possession == TeamSide.HOME) == homeWon) 1.0 else 0.0
        val endOfGamePlay =
            Play(
                gameId = game.gameId,
                playNumber = lastPlay.playNumber + 1,
                homeScore = game.homeScore,
                awayScore = game.awayScore,
                quarter = lastPlay.quarter,
                clock = 0,
                ballLocation = lastPlay.ballLocation,
                possession = lastPlay.possession,
                down = 0,
                yardsToGo = 0,
                defensiveNumber = null,
                offensiveNumber = null,
                offensiveSubmitter = null,
                offensiveSubmitterId = null,
                defensiveSubmitter = null,
                defensiveSubmitterId = null,
                playCall = null,
                result = Scenario.END_OF_GAME,
                actualResult = ActualResult.END_OF_GAME,
                yards = 0,
                playTime = 0,
                runoffTime = 0,
                winProbability = terminalWinProbability,
                winProbabilityAdded = 0.0,
                homeTeam = game.homeTeam,
                awayTeam = game.awayTeam,
                difference = 0,
                timeoutUsed = false,
                offensiveTimeoutCalled = false,
                defensiveTimeoutCalled = false,
                homeTimeouts = game.homeTimeouts,
                awayTimeouts = game.awayTimeouts,
                playFinished = true,
                offensiveResponseSpeed = null,
                defensiveResponseSpeed = null,
            )
        playRepository.save(endOfGamePlay)
        game.winProbability = terminalWinProbability
    }

    fun finalizeCompletedPlay(
        game: Game,
        wasAlreadyFinal: Boolean,
    ): Game =
        finalizeGameInTransaction(game) {
            gameRepository.save(game)
            endGame(game, wasAlreadyFinal)
        }

    private fun getGameById(id: Int): Game = gameRepository.getGameById(id) ?: throw GameNotFoundException("No game found with ID: $id")

    private fun getGameByPlatformId(platformId: ULong) =
        gameRepository.getGameByPlatformId(platformId)
            ?: throw GameNotFoundException("Game not found for Platform ID: $platformId")

    private fun getAllOngoingGames() =
        gameRepository.getAllOngoingGames().ifEmpty {
            throw GameNotFoundException("No ongoing games found")
        }
}
