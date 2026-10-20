package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.domain.game.GameConditions
import com.fcfb.arceus.enums.game.GameCategory
import com.fcfb.arceus.enums.game.GameFilter
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameSort
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.notification.NotificationStatus
import com.fcfb.arceus.enums.notification.NotificationType
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.exception.game.InvalidHalfTimePossessionChangeException
import com.fcfb.arceus.exception.team.NoCoachDiscordIdsFoundException
import com.fcfb.arceus.exception.team.TeamNotFoundException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.game.NotificationOutbox
import com.fcfb.arceus.model.game.PlatformTransport
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.game.NotificationOutboxRepository
import com.fcfb.arceus.repositories.game.PlatformTransportRepository
import com.fcfb.arceus.repositories.game.requireOngoingGames
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.probability.WinProbabilityService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.service.specification.GameSpecificationService
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.game.convertClockToString
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val teamService: TeamService,
    private val discordService: DiscordService,
    private val userService: UserService,
    private val gameStatsService: GameStatsService,
    private val gameSpecificationService: GameSpecificationService,
    private val winProbabilityService: WinProbabilityService,
    private val gameStatsRepository: GameStatsRepository,
    private val gameFinalizationService: GameFinalizationService,
    private val notificationOutboxRepository: NotificationOutboxRepository,
    private val gameSplitTableSyncService: GameSplitTableSyncService,
    private val platformTransportRepository: PlatformTransportRepository,
) {
    private val gameConditions = GameConditions()

    fun saveGame(game: Game): Game = gameRepository.save(game)

    fun updateGameInformation(
        game: Game,
        play: Play,
        homeScore: Int,
        awayScore: Int,
        possession: TeamSide,
        quarter: Int,
        clock: Int,
        ballLocation: Int,
        down: Int,
        yardsToGo: Int,
        homeTimeoutCalled: Boolean,
        awayTimeoutCalled: Boolean,
        timeoutUsed: Boolean,
    ): Game {
        val wasAlreadyFinal = game.gameStatus == GameStatus.FINAL
        val waitingOn = updateWaitingOn(possession)
        updateClockStopped(game, play, clock)
        updateTimeouts(game, homeTimeoutCalled, awayTimeoutCalled, timeoutUsed)
        updatePlayType(game, play)
        updateCloseGame(game, play)
        updateUpsetAlert(game, play)

        if (quarter == 0) {
            updateEndOfRegulationGameValues(game)
        } else if (game.gameStatus == GameStatus.OVERTIME) {
            updateOvertimeValues(game, play, possession, waitingOn, ballLocation, down, yardsToGo, homeScore, awayScore)
        } else if (game.gameStatus == GameStatus.OPENING_KICKOFF) {
            updateStartGameValues(game, clock, possession, quarter, ballLocation, down, yardsToGo, waitingOn)
        } else if (quarter == 3 && clock == 420 && game.gameStatus != GameStatus.HALFTIME) {
            updateHalftimeValues(game, possession, waitingOn, clock)
        } else if (game.gameStatus == GameStatus.HALFTIME) {
            updateStartOfHalfValues(game, clock, possession, quarter, ballLocation, down, yardsToGo, waitingOn)
        } else if (quarter >= 5 && game.gameStatus == GameStatus.IN_PROGRESS) {
            updateStartOfOvertimeValues(game, quarter)
        } else {
            updateNormalPlayValues(game, clock, possession, quarter, ballLocation, down, yardsToGo, waitingOn)
        }

        try {
            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            val gameStats = gameStatsRepository.findByGameId(game.gameId)
            val homeGameStats = gameStats.find { it.team == game.homeTeam }
            val awayGameStats = gameStats.find { it.team == game.awayTeam }

            val homeElo = homeGameStats?.teamElo ?: homeTeam.currentElo
            val awayElo = awayGameStats?.teamElo ?: awayTeam.currentElo
            winProbabilityService.calculateWinProbability(game, play, homeElo, awayElo)
        } catch (e: Exception) {
            Logger().error("Error calculating win probability: ${e.message}")
            play.winProbability = 0.5
            play.winProbabilityAdded = 0.0
        }

        game.homeScore = homeScore
        game.awayScore = awayScore
        game.winProbability = play.winProbability
        game.numPlays = play.playNumber
        game.gameTimer = calculateDelayOfGameTimer()
        game.gameWarning = GameWarning.NONE

        if (game.gameStatus == GameStatus.FINAL) {
            gameFinalizationService.finalizeCompletedPlay(game, wasAlreadyFinal)
        } else {
            gameRepository.save(game)
        }

        return game
    }

    fun updateWithDefensiveNumberSubmission(
        game: Game,
        gamePlay: Play,
    ) {
        game.currentPlayId = gamePlay.playId
        game.waitingOn = game.possession
        game.gameTimer = calculateDelayOfGameTimer()
        gameRepository.save(game)
    }

    fun rollbackPlay(
        game: Game,
        previousPlay: Play,
        gamePlay: Play,
    ) {
        try {
            if (gamePlay.actualResult == ActualResult.DELAY_OF_GAME) {
                if (game.waitingOn == TeamSide.HOME) {
                    game.awayScore -= 8
                    if (game.gameType != GameType.SCRIMMAGE) {
                        val user = userService.getUserByDiscordId(game.homeCoachDiscordIds?.get(0) ?: "")
                        user.delayOfGameInstances -= 1
                        userService.saveUser(user)
                    }
                } else {
                    game.homeScore -= 8
                    if (game.gameType != GameType.SCRIMMAGE) {
                        val user = userService.getUserByDiscordId(game.awayCoachDiscordIds?.get(0) ?: "")
                        user.delayOfGameInstances -= 1
                        userService.saveUser(user)
                    }
                }
                when (previousPlay.playCall) {
                    PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_ONSIDE, PlayCall.KICKOFF_SQUIB -> {
                        game.currentPlayType = PlayType.KICKOFF
                    }
                    PlayCall.PAT, PlayCall.TWO_POINT -> {
                        game.currentPlayType = PlayType.PAT
                    }
                    else -> {
                        game.currentPlayType = PlayType.NORMAL
                    }
                }
            }
            if (gameConditions.isOffensiveTouchdownPlay(gamePlay.actualResult)) {
                if (gamePlay.possession == TeamSide.HOME) {
                    game.homeScore -= 6
                } else {
                    game.awayScore -= 6
                }
                game.currentPlayType = PlayType.NORMAL
            }
            if (gameConditions.isDefensiveTouchdownPlay(gamePlay.actualResult)) {
                if (gamePlay.possession == TeamSide.AWAY) {
                    game.homeScore -= 6
                } else {
                    game.awayScore -= 6
                }
                game.currentPlayType = PlayType.NORMAL
            }
            if (gamePlay.actualResult == ActualResult.GOOD) {
                if (gamePlay.playCall == PlayCall.PAT) {
                    if (gamePlay.possession == TeamSide.HOME) {
                        game.homeScore -= 1
                    } else {
                        game.awayScore -= 1
                    }
                    game.currentPlayType = PlayType.PAT
                } else if (gamePlay.playCall == PlayCall.FIELD_GOAL) {
                    if (gamePlay.possession == TeamSide.HOME) {
                        game.homeScore -= 3
                    } else {
                        game.awayScore -= 3
                    }
                    game.currentPlayType = PlayType.NORMAL
                }
            }
            if (gamePlay.actualResult == ActualResult.SUCCESS) {
                if (gamePlay.possession == TeamSide.HOME) {
                    game.homeScore -= 2
                } else {
                    game.awayScore -= 2
                }
                game.currentPlayType = PlayType.PAT
            }
            if (gamePlay.actualResult == ActualResult.DEFENSE_TWO_POINT) {
                if (gamePlay.possession == TeamSide.HOME) {
                    game.awayScore -= 2
                } else {
                    game.homeScore -= 2
                }
                game.currentPlayType = PlayType.PAT
            }
            if (gamePlay.actualResult == ActualResult.SAFETY) {
                if (gamePlay.possession == TeamSide.HOME) {
                    game.awayScore -= 2
                } else {
                    game.homeScore -= 2
                }
                game.currentPlayType = PlayType.NORMAL
            }
            if (previousPlay.playCall == PlayCall.KICKOFF_NORMAL ||
                previousPlay.playCall == PlayCall.KICKOFF_ONSIDE ||
                previousPlay.playCall == PlayCall.KICKOFF_SQUIB
            ) {
                game.currentPlayType = PlayType.KICKOFF
            }
            if (gamePlay.offensiveTimeoutCalled) {
                if (gamePlay.possession == TeamSide.HOME) {
                    game.homeTimeouts--
                } else {
                    game.awayTimeouts--
                }
            }
            if (game.gameStatus == GameStatus.FINAL) {
                if (game.quarter <= 4) {
                    game.gameStatus = GameStatus.IN_PROGRESS
                } else {
                    game.gameStatus = GameStatus.OVERTIME
                }
            }

            val newCurrentPlay =
                playRepository.getCurrentPlay(game.gameId)
                    ?: playRepository.getPreviousPlay(game.gameId)
                    ?: previousPlay
            game.currentPlayId = newCurrentPlay.playId
            game.possession = previousPlay.possession
            game.quarter = previousPlay.quarter
            game.clock = convertClockToString(previousPlay.clock)
            game.ballLocation = previousPlay.ballLocation
            game.down = previousPlay.down
            game.yardsToGo = previousPlay.yardsToGo
            game.waitingOn = if (previousPlay.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
            game.gameTimer = calculateDelayOfGameTimer()
            gameStatsService.generateGameStats(game.gameId)
            saveGame(game)
        } catch (e: Exception) {
            Logger().error(
                "Error rolling back play.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Error Message: ${e.message ?: "Unknown error"}",
            )
            throw e
        }
    }

    private fun endOvertimePeriod(game: Game) {
        game.overtimeHalf = 1
        game.ballLocation = 75
        game.down = 1
        game.yardsToGo = 10
        game.quarter += 1
        game.homeTimeouts = 1
        game.awayTimeouts = 1
        game.waitingOn = if (game.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
    }

    fun chewGame(game: Game): Game = setGameMode(game, GameMode.CHEW)

    fun unchewGame(game: Game): Game = setGameMode(game, GameMode.NORMAL)

    private fun setGameMode(
        game: Game,
        gameMode: GameMode,
    ): Game {
        try {
            game.gameMode = gameMode
            game.gameModeSetBy = actingUser()
            game.gameModeSetAt = LocalDateTime.now()
            saveGame(game)
            discordService.notifyGameModeChange(game)
            Logger().info(
                "Game set to ${gameMode.description.lowercase()} mode by ${game.gameModeSetBy}.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Game Type: ${game.gameType}\n" +
                    "Game Status: ${game.gameStatus}\n" +
                    "Home team: ${game.homeTeam}\n" +
                    "Away team: ${game.awayTeam}\n",
            )
            return game
        } catch (e: Exception) {
            Logger().error("Error in ${game.gameId}: ${e.message ?: "Unknown error"}")
            throw e
        }
    }

    private fun actingUser(): String {
        val userId = AuthContext().currentUserId() ?: return AuthContext().currentPrincipal()
        return userService.getUserById(userId).username
    }

    fun chewAllGames(): List<Game> = gameRepository.requireOngoingGames().map { chewGame(it) }

    fun unchewAllGames(): List<Game> = gameRepository.requireOngoingGames().map { unchewGame(it) }

    fun deleteOngoingGame(channelId: ULong): Boolean {
        val game = getGameByPlatformId(channelId)
        deleteGameById(game.gameId)
        return true
    }

    fun deleteGameById(gameId: Int): Boolean {
        gameRepository.deleteById(gameId)
        gameStatsService.deleteByGameId(gameId)
        playRepository.deleteAllPlaysByGameId(gameId)
        Logger().info("Game deleted.\nGame ID: $gameId")
        return true
    }

    fun calculateDelayOfGameTimer(): String? {
        val now = ZonedDateTime.now(ZoneId.of("America/New_York"))
        val futureTime = now.plusHours(18)
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
        return futureTime.format(formatter)
    }

    fun updateRequestMessageId(
        gameId: Int,
        requestMessageId: String,
    ): Game {
        val game = getGameById(gameId)

        val requestMessageIdList =
            if (requestMessageId.contains(",")) {
                requestMessageId.split(",").let { messageIds -> listOf(messageIds[0], messageIds[1]) }
            } else {
                listOf(requestMessageId)
            }
        game.requestMessageId = requestMessageIdList
        saveGame(game)
        syncPlatformTransport(game)
        return game
    }

    fun updateLastMessageTimestamp(gameId: Int): Game {
        val game = getGameById(gameId)
        val timestamp = Timestamp.from(Instant.now()).toString()
        game.lastMessageTimestamp = timestamp
        saveGame(game)
        syncPlatformTransport(game)
        return game
    }

    private fun syncPlatformTransport(game: Game) {
        platformTransportRepository.save(
            PlatformTransport(
                gameId = game.gameId,
                homePlatform = game.homePlatform,
                homePlatformId = game.homePlatformId,
                awayPlatform = game.awayPlatform,
                awayPlatformId = game.awayPlatformId,
                requestMessageId = game.requestMessageId,
                lastMessageTimestamp = game.lastMessageTimestamp,
            ),
        )
    }

    fun resetDelayOfGameTimer(gameId: Int): Game {
        AuthContext().requireAdminOrCommissioner()
        val game = getGameById(gameId)
        game.gameTimer = calculateDelayOfGameTimer()
        game.gameWarning = GameWarning.NONE
        saveGame(game)
        return game
    }

    fun updateGame(game: Game): Game {
        val existingGame = getGameById(game.gameId)

        existingGame.apply {
            this.gameId = game.gameId
            this.homeTeam = game.homeTeam
            this.awayTeam = game.awayTeam
            this.homeCoaches = game.homeCoaches
            this.awayCoaches = game.awayCoaches
            this.homeCoachDiscordIds = game.homeCoachDiscordIds
            this.awayCoachDiscordIds = game.awayCoachDiscordIds
            this.homeOffensivePlaybook = game.homeOffensivePlaybook
            this.awayOffensivePlaybook = game.awayOffensivePlaybook
            this.homeDefensivePlaybook = game.homeDefensivePlaybook
            this.awayDefensivePlaybook = game.awayDefensivePlaybook
            this.homeScore = game.homeScore
            this.awayScore = game.awayScore
            this.possession = game.possession
            this.quarter = game.quarter
            this.clock = game.clock
            this.ballLocation = game.ballLocation
            this.down = game.down
            this.yardsToGo = game.yardsToGo
            this.tvChannel = game.tvChannel
            this.homeTeamRank = game.homeTeamRank
            this.homeWins = game.homeWins
            this.homeLosses = game.homeLosses
            this.awayTeamRank = game.awayTeamRank
            this.awayWins = game.awayWins
            this.awayLosses = game.awayLosses
            this.subdivision = game.subdivision
            this.timestamp = game.timestamp
            this.winProbability = game.winProbability
            this.season = game.season
            this.week = game.week
            this.waitingOn = game.waitingOn
            this.numPlays = game.numPlays
            this.homeTimeouts = game.homeTimeouts
            this.awayTimeouts = game.awayTimeouts
            this.coinTossWinner = game.coinTossWinner
            this.coinTossChoice = game.coinTossChoice
            this.overtimeCoinTossWinner = game.overtimeCoinTossWinner
            this.overtimeCoinTossChoice = game.overtimeCoinTossChoice
            this.homePlatform = game.homePlatform
            this.homePlatformId = game.homePlatformId
            this.awayPlatform = game.awayPlatform
            this.awayPlatformId = game.awayPlatformId
            this.lastMessageTimestamp = game.lastMessageTimestamp
            this.gameTimer = game.gameTimer
            this.gameWarning = game.gameWarning
            this.currentPlayType = game.currentPlayType
            this.currentPlayId = game.currentPlayId
            this.clockStopped = game.clockStopped
            this.requestMessageId = game.requestMessageId
            this.gameStatus = game.gameStatus
            this.gameType = game.gameType
            this.gameMode = game.gameMode
            this.overtimeHalf = game.overtimeHalf
            this.closeGame = game.closeGame
            this.closeGamePinged = game.closeGamePinged
            this.upsetAlert = game.upsetAlert
            this.upsetAlertPinged = game.upsetAlertPinged
        }
        saveGame(existingGame)
        gameSplitTableSyncService.sync(existingGame)
        return existingGame
    }

    private fun updateTimeouts(
        game: Game,
        homeTimeoutCalled: Boolean,
        awayTimeoutCalled: Boolean,
        timeoutUsed: Boolean,
    ) {
        when {
            homeTimeoutCalled && timeoutUsed -> game.homeTimeouts -= 1
            awayTimeoutCalled && timeoutUsed -> game.awayTimeouts -= 1
        }
    }

    private fun updateWaitingOn(possession: TeamSide): TeamSide {
        return if (possession == TeamSide.HOME) {
            TeamSide.AWAY
        } else {
            TeamSide.HOME
        }
    }

    private fun updatePlayType(
        game: Game,
        play: Play,
    ) {
        if (gameConditions.isTouchdownPlay(play.actualResult)) {
            game.currentPlayType = PlayType.PAT
        } else if (play.actualResult == ActualResult.SAFETY ||
            (play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD) ||
            play.playCall == PlayCall.PAT ||
            play.playCall == PlayCall.TWO_POINT
        ) {
            if (game.gameStatus == GameStatus.OVERTIME) {
                game.currentPlayType = PlayType.NORMAL
            } else {
                game.currentPlayType = PlayType.KICKOFF
            }
        } else {
            game.currentPlayType = PlayType.NORMAL
        }
    }

    private fun updateStartGameValues(
        game: Game,
        clock: Int,
        possession: TeamSide,
        quarter: Int,
        ballLocation: Int,
        down: Int,
        yardsToGo: Int,
        waitingOn: TeamSide,
    ) {
        game.gameStatus = GameStatus.IN_PROGRESS
        game.clock = convertClockToString(clock)
        game.possession = possession
        game.quarter = quarter
        game.ballLocation = ballLocation
        game.down = down
        game.yardsToGo = yardsToGo
        game.waitingOn = waitingOn
    }

    private fun updateHalftimeValues(
        game: Game,
        possession: TeamSide,
        waitingOn: TeamSide,
        clock: Int,
    ) {
        game.homeTimeouts = 3
        game.awayTimeouts = 3
        game.gameStatus = GameStatus.HALFTIME
        game.currentPlayType = PlayType.KICKOFF
        game.ballLocation = 35
        game.clock = convertClockToString(clock)
        game.possession = possession
        game.quarter = 3
        game.down = 1
        game.yardsToGo = 10
        game.waitingOn = waitingOn
    }

    private fun updateStartOfHalfValues(
        game: Game,
        clock: Int,
        possession: TeamSide,
        quarter: Int,
        ballLocation: Int,
        down: Int,
        yardsToGo: Int,
        waitingOn: TeamSide,
    ) {
        game.gameStatus = GameStatus.IN_PROGRESS
        game.clock = convertClockToString(clock)
        game.possession = possession
        game.quarter = quarter
        game.ballLocation = ballLocation
        game.down = down
        game.yardsToGo = yardsToGo
        game.waitingOn = waitingOn
    }

    private fun updateEndOfRegulationGameValues(game: Game) {
        game.quarter = 4
        game.clock = "0:00"
        game.clockStopped = true
        game.gameStatus = GameStatus.FINAL
    }

    private fun updateNormalPlayValues(
        game: Game,
        clock: Int,
        possession: TeamSide,
        quarter: Int,
        ballLocation: Int,
        down: Int,
        yardsToGo: Int,
        waitingOn: TeamSide,
    ) {
        game.clock = convertClockToString(clock)
        game.possession = possession
        game.quarter = quarter
        game.ballLocation = ballLocation
        game.down = down
        game.yardsToGo = yardsToGo
        game.waitingOn = waitingOn
    }

    private fun updateStartOfOvertimeValues(
        game: Game,
        quarter: Int,
    ) {
        game.clock = "0:00"
        game.quarter = quarter
        game.gameStatus = GameStatus.END_OF_REGULATION
        game.currentPlayType = PlayType.NORMAL
        game.ballLocation = 75
        game.down = 1
        game.yardsToGo = 10
        game.overtimeHalf = 1
        game.homeTimeouts = 1
        game.awayTimeouts = 1
    }

    private fun updateOvertimeValues(
        game: Game,
        play: Play,
        possession: TeamSide,
        waitingOn: TeamSide,
        ballLocation: Int,
        down: Int,
        yardsToGo: Int,
        homeScore: Int,
        awayScore: Int,
    ) {
        game.clock = "0:00"
        if (gameConditions.isEndOfOvertimeHalf(play)) {
            if (game.overtimeHalf == 1) {
                if (play.actualResult == ActualResult.TOUCHDOWN) {
                    game.possession = possession
                    game.waitingOn = waitingOn
                    game.ballLocation = ballLocation
                    game.down = down
                    game.yardsToGo = yardsToGo
                } else if (
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
                    play.actualResult == ActualResult.KICK_SIX
                ) {
                    game.gameStatus = GameStatus.FINAL
                } else {
                    game.overtimeHalf = 2
                    game.possession =
                        if (game.possession == TeamSide.HOME) {
                            TeamSide.AWAY
                        } else {
                            TeamSide.HOME
                        }
                    game.ballLocation = 75
                    game.down = 1
                    game.yardsToGo = 10
                    game.waitingOn = if (game.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
                }
            } else {
                if (homeScore != awayScore || game.currentPlayType == PlayType.PAT) {
                    if (gameConditions.isGameMathmaticallyOver(play, homeScore, awayScore)) {
                        game.possession = possession
                        game.waitingOn = waitingOn
                        game.ballLocation = ballLocation
                        game.down = down
                        game.yardsToGo = yardsToGo
                    } else {
                        game.gameStatus = GameStatus.FINAL
                    }
                } else {
                    endOvertimePeriod(game)
                }
            }
        } else {
            game.possession = possession
            game.waitingOn = waitingOn
            game.ballLocation = ballLocation
            game.down = down
            game.yardsToGo = yardsToGo
        }
    }

    fun subCoachIntoGame(
        gameId: Int,
        team: String,
        discordId: String,
    ): Game {
        val game = getGameById(gameId)
        val userData = userService.getUserDTOByDiscordId(discordId)
        val coach = userData.discordTag

        when (team.lowercase()) {
            game.homeTeam.lowercase() -> {
                game.homeCoaches = listOf(coach)
                game.homeCoachDiscordIds = listOf(userData.discordId ?: throw NoCoachDiscordIdsFoundException())
            }
            game.awayTeam.lowercase() -> {
                game.awayCoaches = listOf(coach)
                game.awayCoachDiscordIds = listOf(userData.discordId ?: throw NoCoachDiscordIdsFoundException())
            }
            else -> {
                throw TeamNotFoundException("$team not found in game $gameId")
            }
        }
        saveGame(game)
        return game
    }

    fun getGameByRequestMessageId(requestMessageId: String) =
        gameRepository.getGameByRequestMessageId("\"$requestMessageId\"")
            ?: throw GameNotFoundException("Game not found for Request Message ID: $requestMessageId")

    fun getGameByPlatformId(platformId: ULong) =
        gameRepository.getGameByPlatformId(platformId)
            ?: throw GameNotFoundException("Game not found for Platform ID: $platformId")

    fun getGameById(id: Int): Game = gameRepository.getGameById(id) ?: throw GameNotFoundException("No game found with ID: $id")

    fun chewGameByPlatformId(channelId: ULong): Game = chewGame(getGameByPlatformId(channelId))

    fun chewGameByGameId(gameId: Int): Game = chewGame(getGameById(gameId))

    fun unchewGameByPlatformId(channelId: ULong): Game = unchewGame(getGameByPlatformId(channelId))

    fun unchewGameByGameId(gameId: Int): Game = unchewGame(getGameById(gameId))

    fun getFilteredGames(
        filters: List<GameFilter>?,
        category: GameCategory?,
        conference: String?,
        season: Int?,
        week: Int?,
        gameMode: GameMode?,
        sort: GameSort,
        pageable: Pageable,
    ): Page<Game> {
        val filterSpec = gameSpecificationService.createSpecification(filters ?: emptyList(), category, conference, season, week, gameMode)
        val sortOrders = gameSpecificationService.createSort(sort)
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )

        return gameRepository.findAll(filterSpec, sortedPageable)
            ?: throw GameNotFoundException(
                "No games found for the following filters: " +
                    "filters = $filters, " +
                    "category = $category, " +
                    "conference = $conference, " +
                    "season = $season, " +
                    "week = $week, " +
                    "gameMode = $gameMode",
            )
    }

    fun findExpiredTimers() =
        gameRepository.findExpiredTimers().ifEmpty {
            Logger().debug("No games found with expired timers")
            emptyList()
        }

    fun findGamesToWarnFirstInstance() = gameRepository.findGamesToWarnFirstInstance()

    fun findGamesToWarnSecondInstance() = gameRepository.findGamesToWarnSecondInstance()

    fun updateGameAsWarned(
        gameId: Int,
        instance: Int,
    ) = if (instance == 1) {
        gameRepository.updateGameAsFirstWarning(gameId)
    } else {
        gameRepository.updateGameAsSecondWarning(gameId)
    }

    fun findPendingDogNotifications(): List<NotificationOutbox> =
        notificationOutboxRepository.findByTypeAndStatus(NotificationType.DOG_OUT, NotificationStatus.PENDING)

    fun markPendingDogNotification(
        gameId: Int,
        isDelayOfGameOut: Boolean,
    ) {
        val notification =
            notificationOutboxRepository.findByGameIdAndType(gameId, NotificationType.DOG_OUT)
                ?: NotificationOutbox(gameId, NotificationType.DOG_OUT, isDelayOfGameOut.toString())
        notification.payload = isDelayOfGameOut.toString()
        notification.status = NotificationStatus.PENDING
        notification.updatedAt = LocalDateTime.now()
        notificationOutboxRepository.save(notification)
    }

    fun clearPendingDogNotification(gameId: Int) {
        notificationOutboxRepository.deleteByGameIdAndType(gameId, NotificationType.DOG_OUT)
    }

    fun markCloseGamePinged(gameId: Int) = gameRepository.markCloseGamePinged(gameId)

    fun markUpsetAlertPinged(gameId: Int) = gameRepository.markUpsetAlertPinged(gameId)

    fun getAllGames() =
        gameRepository.getAllGames().ifEmpty {
            throw GameNotFoundException("No games found when getting all games")
        }

    fun getGamesByIds(gameIds: List<Int>): List<Game> = gameRepository.findAllById(gameIds).toList()

    fun getGamesWithTeams(
        teams: List<Team>,
        season: Int,
        week: Int,
    ): List<Game> {
        val games = mutableListOf<Game>()
        for (team in teams) {
            val game = gameRepository.getGamesByTeamSeasonAndWeek(team.name ?: "", season, week)
            if (game != null) {
                games.add(game)
            } else {
                throw GameNotFoundException("No games found for ${team.name} in season $season week $week")
            }
        }
        return games
    }

    fun getGameBySeasonAndMatchup(
        season: Int,
        firstTeam: String,
        secondTeam: String,
    ) = gameRepository.getGamesBySeasonAndMatchup(season, firstTeam, secondTeam)

    fun handleHalfTimePossessionChange(game: Game): TeamSide {
        return if (game.coinTossWinner == TeamSide.HOME && game.coinTossChoice == CoinTossChoice.DEFER) {
            TeamSide.AWAY
        } else if (game.coinTossWinner == TeamSide.HOME && game.coinTossChoice == CoinTossChoice.RECEIVE) {
            TeamSide.HOME
        } else if (game.coinTossWinner == TeamSide.AWAY && game.coinTossChoice == CoinTossChoice.DEFER) {
            TeamSide.HOME
        } else if (game.coinTossWinner == TeamSide.AWAY && game.coinTossChoice == CoinTossChoice.RECEIVE) {
            TeamSide.AWAY
        } else {
            throw InvalidHalfTimePossessionChangeException()
        }
    }

    private fun updateCloseGame(
        game: Game,
        play: Play,
    ) {
        game.closeGame = abs(game.homeScore - game.awayScore) <= 8 &&
            play.quarter >= 4 &&
            play.clock <= 210
    }

    private fun updateUpsetAlert(
        game: Game,
        play: Play,
    ) {
        val homeTeamRanking = game.homeTeamRank ?: 100
        val awayTeamRanking = game.awayTeamRank ?: 100

        if ((
                (game.homeScore <= game.awayScore && homeTeamRanking < awayTeamRanking) ||
                    (game.awayScore <= game.homeScore && awayTeamRanking < homeTeamRanking)
            ) &&
            game.quarter >= 4 &&
            play.clock <= 210
        ) {
            game.upsetAlert = true
        }
        if ((
                (abs(game.homeScore - game.awayScore) <= 8 && homeTeamRanking < awayTeamRanking) ||
                    (abs(game.awayScore - game.homeScore) <= 8 && awayTeamRanking < homeTeamRanking)
            ) &&
            game.quarter >= 4 &&
            play.clock <= 210
        ) {
            game.upsetAlert = true
        }
        game.upsetAlert = false
    }

    fun getDifference(
        offensiveNumber: Int,
        defensiveNumber: Int,
    ): Int = gameConditions.getDifference(offensiveNumber, defensiveNumber)

    fun isTouchdownPlay(actualResult: ActualResult?): Boolean = gameConditions.isTouchdownPlay(actualResult)

    private fun updateClockStopped(
        game: Game,
        play: Play,
        clock: Int,
    ) {
        game.clockStopped = play.playCall == PlayCall.SPIKE || play.result == Scenario.INCOMPLETE ||
            play.actualResult == ActualResult.TURNOVER_ON_DOWNS ||
            play.actualResult == ActualResult.TOUCHDOWN || play.playCall == PlayCall.FIELD_GOAL ||
            play.playCall == PlayCall.PAT || play.playCall == PlayCall.KICKOFF_NORMAL ||
            play.playCall == PlayCall.KICKOFF_ONSIDE || play.playCall == PlayCall.KICKOFF_SQUIB ||
            play.playCall == PlayCall.PUNT || play.actualResult == ActualResult.TURNOVER ||
            play.actualResult == ActualResult.TURNOVER_TOUCHDOWN || play.actualResult == ActualResult.SAFETY ||
            game.gameStatus == GameStatus.OVERTIME || game.gameStatus == GameStatus.HALFTIME

        if (clock == 420) {
            game.clockStopped = true
        }
    }
}
