package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.StartRequest
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.system.Platform.DISCORD
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.specification.GameSpecificationService
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter
import com.fcfb.arceus.service.specification.GameSpecificationService.GameSort
import com.fcfb.arceus.util.GameNotFoundException
import com.fcfb.arceus.util.InvalidCoinTossChoiceException
import com.fcfb.arceus.util.InvalidHalfTimePossessionChangeException
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.NoCoachDiscordIdsFoundException
import com.fcfb.arceus.util.NoCoachesFoundException
import com.fcfb.arceus.util.NoGameFoundException
import com.fcfb.arceus.util.TeamNotFoundException
import com.fcfb.arceus.util.UnableToCreateGameThreadException
import com.fcfb.arceus.util.UnableToDeleteGameException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.lang.Thread.sleep
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Random
import kotlin.math.abs

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val teamService: TeamService,
    private val discordService: DiscordService,
    private val userService: UserService,
    private val gameStatsService: GameStatsService,
    private val seasonService: SeasonService,
    private val scheduleService: ScheduleService,
    private val gameSpecificationService: GameSpecificationService,
    private val recordService: RecordService,
    private val seasonStatsService: SeasonStatsService,
    private val winProbabilityService: WinProbabilityService,
    private val vegasOddsService: VegasOddsService,
    private val gameStatsRepository: GameStatsRepository,
) {
    /**
     * Save a game state
     */
    fun saveGame(game: Game): Game = gameRepository.save(game)

    suspend fun startSingleGame(
        startRequest: StartRequest,
        week: Int?,
    ): Game {
        val game = startNormalGame(startRequest, week)
        if (startRequest.gameType != GameType.SCRIMMAGE) {
            scheduleService.markManuallyStartedGameAsStarted(game)
        }
        return game
    }

    /**
     * Start a game
     * @param startRequest
     * @return
     */
    private suspend fun startNormalGame(
        startRequest: StartRequest,
        week: Int?,
    ): Game {
        try {
            val homeTeamData = teamService.getTeamByName(startRequest.homeTeam)
            val awayTeamData = teamService.getTeamByName(startRequest.awayTeam)

            val formattedDateTime = calculateDelayOfGameTimer()

            // Validate request fields
            val homeTeam = homeTeamData.name ?: throw TeamNotFoundException("Home team not found")
            val awayTeam = awayTeamData.name ?: throw TeamNotFoundException("Away team not found")

            val homeCoachUsernames = homeTeamData.coachUsernames ?: throw NoCoachesFoundException()
            val awayCoachUsernames = awayTeamData.coachUsernames ?: throw NoCoachesFoundException()
            val homeCoachDiscordIds = homeTeamData.coachDiscordIds ?: throw NoCoachDiscordIdsFoundException()
            val awayCoachDiscordIds = awayTeamData.coachDiscordIds ?: throw NoCoachDiscordIdsFoundException()

            if (homeCoachUsernames.isEmpty() || awayCoachUsernames.isEmpty()
            ) {
                throw NoCoachesFoundException()
            }

            if (homeCoachDiscordIds.isEmpty() || awayCoachDiscordIds.isEmpty()
            ) {
                throw NoCoachDiscordIdsFoundException()
            }

            val homeOffensivePlaybook = homeTeamData.offensivePlaybook
            val awayOffensivePlaybook = awayTeamData.offensivePlaybook
            val homeDefensivePlaybook = homeTeamData.defensivePlaybook
            val awayDefensivePlaybook = awayTeamData.defensivePlaybook
            val subdivision = startRequest.subdivision
            val homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
            val awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

            val (season, currentWeek) = getCurrentSeasonAndWeek(startRequest, week)
            val (homeTeamRank, awayTeamRank) = teamService.getTeamRanks(homeTeamData.id, awayTeamData.id)

            // Calculate Vegas odds for the game
            val vegasOdds = vegasOddsService.calculateVegasOdds(homeTeamData, awayTeamData)

            // Create and save the Game object and Stats object
            val newGame =
                withContext(Dispatchers.IO) {
                    gameRepository.save(
                        Game(
                            homeTeam = homeTeam,
                            awayTeam = awayTeam,
                            homeCoaches = homeCoachUsernames,
                            awayCoaches = awayCoachUsernames,
                            homeCoachDiscordIds = homeCoachDiscordIds,
                            awayCoachDiscordIds = awayCoachDiscordIds,
                            homeOffensivePlaybook = homeOffensivePlaybook,
                            awayOffensivePlaybook = awayOffensivePlaybook,
                            homeDefensivePlaybook = homeDefensivePlaybook,
                            awayDefensivePlaybook = awayDefensivePlaybook,
                            homeScore = 0,
                            awayScore = 0,
                            possession = TeamSide.HOME,
                            quarter = 1,
                            clock = "7:00",
                            ballLocation = 35,
                            down = 1,
                            yardsToGo = 10,
                            tvChannel = startRequest.tvChannel,
                            homeTeamRank = homeTeamRank,
                            homeWins = homeTeamData.currentWins,
                            homeLosses = homeTeamData.currentLosses,
                            awayTeamRank = awayTeamRank,
                            awayWins = awayTeamData.currentWins,
                            awayLosses = awayTeamData.currentLosses,
                            subdivision = subdivision,
                            timestamp = LocalDateTime.now().toString(),
                            winProbability = 0.0,
                            season = season,
                            week = currentWeek,
                            waitingOn = TeamSide.AWAY,
                            numPlays = 0,
                            homeTimeouts = 3,
                            awayTimeouts = 3,
                            coinTossWinner = null,
                            coinTossChoice = null,
                            overtimeCoinTossWinner = null,
                            overtimeCoinTossChoice = null,
                            homePlatform = homePlatform,
                            homePlatformId = null,
                            awayPlatform = awayPlatform,
                            awayPlatformId = null,
                            lastMessageTimestamp = null,
                            gameTimer = formattedDateTime,
                            gameWarning = GameWarning.NONE,
                            currentPlayType = PlayType.KICKOFF,
                            currentPlayId = 0,
                            clockStopped = true,
                            requestMessageId = null,
                            gameType = startRequest.gameType,
                            gameStatus = GameStatus.PREGAME,
                            gameMode = GameMode.NORMAL,
                            overtimeHalf = 0,
                            closeGame = false,
                            closeGamePinged = false,
                            upsetAlert = false,
                            upsetAlertPinged = false,
                            homeVegasSpread = vegasOdds.homeSpread,
                            awayVegasSpread = vegasOdds.awaySpread,
                        ),
                    )
                }

            // Create a new Discord thread
            val discordData = createDiscordThread(newGame)
            newGame.homePlatformId = discordData[0]
            newGame.awayPlatformId = discordData[0]
            newGame.requestMessageId = listOf(discordData[1])

            // Save the updated entity and create game stats
            withContext(Dispatchers.IO) {
                gameRepository.save(newGame)
            }
            gameStatsService.createGameStats(newGame)

            Logger.info(
                "Game started.\n" +
                    "Game ID: ${newGame.gameId}\n" +
                    "Game Type: ${newGame.gameType}\n" +
                    "Game Status: ${newGame.gameStatus}\n" +
                    "Home team: ${newGame.homeTeam}\n" +
                    "Away team: ${newGame.awayTeam}",
            )
            return newGame
        } catch (e: Exception) {
            Logger.error(
                "Error starting game.\n" +
                    "Error Message: ${e.message!!}\n" +
                    "Game Type: ${startRequest.gameType}\n" +
                    "Home team: ${startRequest.homeTeam}\n" +
                    "Away team: ${startRequest.awayTeam}",
            )
            throw e
        }
    }

    /**
     * Start an overtime game
     * @param startRequest
     * @return
     */
    suspend fun startOvertimeGame(startRequest: StartRequest): Game {
        try {
            val homeTeamData = teamService.getTeamByName(startRequest.homeTeam)
            val awayTeamData = teamService.getTeamByName(startRequest.awayTeam)

            val formattedDateTime = calculateDelayOfGameTimer()

            // Validate request fields
            val homeTeam = homeTeamData.name ?: throw TeamNotFoundException("Home team not found")
            val awayTeam = awayTeamData.name ?: throw TeamNotFoundException("Away team not found")

            val homeCoachUsernames = homeTeamData.coachUsernames ?: throw NoCoachesFoundException()
            val awayCoachUsernames = awayTeamData.coachUsernames ?: throw NoCoachesFoundException()
            val homeCoachDiscordIds = homeTeamData.coachDiscordIds ?: throw NoCoachDiscordIdsFoundException()
            val awayCoachDiscordIds = awayTeamData.coachDiscordIds ?: throw NoCoachDiscordIdsFoundException()

            if (homeCoachUsernames.isEmpty() || awayCoachUsernames.isEmpty()
            ) {
                throw NoCoachesFoundException()
            }

            if (homeCoachDiscordIds.isEmpty() || awayCoachDiscordIds.isEmpty()
            ) {
                throw NoCoachDiscordIdsFoundException()
            }

            val homeOffensivePlaybook = homeTeamData.offensivePlaybook
            val awayOffensivePlaybook = awayTeamData.offensivePlaybook
            val homeDefensivePlaybook = homeTeamData.defensivePlaybook
            val awayDefensivePlaybook = awayTeamData.defensivePlaybook
            val subdivision = startRequest.subdivision
            val homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
            val awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

            val (homeTeamRank, awayTeamRank) = teamService.getTeamRanks(homeTeamData.id, awayTeamData.id)

            // Create and save the Game object and Stats object
            val newGame =
                withContext(Dispatchers.IO) {
                    gameRepository.save(
                        Game(
                            homeTeam = homeTeam,
                            awayTeam = awayTeam,
                            homeCoaches = homeCoachUsernames,
                            awayCoaches = awayCoachUsernames,
                            homeCoachDiscordIds = homeCoachDiscordIds,
                            awayCoachDiscordIds = awayCoachDiscordIds,
                            homeOffensivePlaybook = homeOffensivePlaybook,
                            awayOffensivePlaybook = awayOffensivePlaybook,
                            homeDefensivePlaybook = homeDefensivePlaybook,
                            awayDefensivePlaybook = awayDefensivePlaybook,
                            homeScore = 0,
                            awayScore = 0,
                            possession = TeamSide.HOME,
                            quarter = 5,
                            clock = "0:00",
                            ballLocation = 75,
                            down = 1,
                            yardsToGo = 10,
                            tvChannel = startRequest.tvChannel,
                            homeTeamRank = homeTeamRank,
                            homeWins = homeTeamData.currentWins,
                            homeLosses = homeTeamData.currentLosses,
                            awayTeamRank = awayTeamRank,
                            awayWins = awayTeamData.currentWins,
                            awayLosses = awayTeamData.currentLosses,
                            subdivision = subdivision,
                            timestamp = LocalDateTime.now().toString(),
                            winProbability = 0.0,
                            season = null,
                            week = null,
                            waitingOn = TeamSide.AWAY,
                            numPlays = 0,
                            homeTimeouts = 1,
                            awayTimeouts = 1,
                            coinTossWinner = null,
                            coinTossChoice = null,
                            overtimeCoinTossWinner = null,
                            overtimeCoinTossChoice = null,
                            homePlatform = homePlatform,
                            homePlatformId = null,
                            awayPlatform = awayPlatform,
                            awayPlatformId = null,
                            lastMessageTimestamp = null,
                            gameTimer = formattedDateTime,
                            gameWarning = GameWarning.NONE,
                            currentPlayType = PlayType.NORMAL,
                            currentPlayId = 0,
                            clockStopped = true,
                            requestMessageId = null,
                            gameType = GameType.SCRIMMAGE,
                            gameStatus = GameStatus.END_OF_REGULATION,
                            gameMode = GameMode.NORMAL,
                            overtimeHalf = 1,
                            closeGame = false,
                            closeGamePinged = false,
                            upsetAlert = false,
                            upsetAlertPinged = false,
                        ),
                    )
                }

            // Create a new Discord thread
            val discordData = createDiscordThread(newGame)
            newGame.homePlatformId = discordData[0]
            newGame.awayPlatformId = discordData[0]
            newGame.requestMessageId = listOf(discordData[1])

            // Save the updated entity and create game stats
            withContext(Dispatchers.IO) {
                gameRepository.save(newGame)
            }
            gameStatsService.createGameStats(newGame)

            Logger.info(
                "Overtime game started.\n" +
                    "Game ID: ${newGame.gameId}\n" +
                    "Game Type: ${newGame.gameType}\n" +
                    "Game Status: ${newGame.gameStatus}\n" +
                    "Home team: ${newGame.homeTeam}\n" +
                    "Away team: ${newGame.awayTeam}",
            )
            return newGame
        } catch (e: Exception) {
            Logger.error(
                "Error starting overtime game.\n" +
                    "Error Message: ${e.message!!}\n" +
                    "Game Type: ${startRequest.gameType}\n" +
                    "Home team: ${startRequest.homeTeam}\n" +
                    "Away team: ${startRequest.awayTeam}",
            )
            throw e
        }
    }

    /**
     * Update game information based on the result of a play
     * @param game
     * @param play
     * @param homeScore
     * @param awayScore
     * @param possession
     * @param quarter
     * @param clock
     * @param ballLocation
     * @param down
     * @param yardsToGo
     * @param homeTimeoutCalled
     * @param awayTimeoutCalled
     * @param timeoutUsed
     */
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
        val waitingOn = updateWaitingOn(possession)
        updateClockStopped(game, play, clock)
        updateTimeouts(game, homeTimeoutCalled, awayTimeoutCalled, timeoutUsed)
        updatePlayType(game, play)
        updateCloseGame(game, play)
        updateUpsetAlert(game, play)

        // Update the quarter/overtime stuff
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

        // Calculate Win Probability
        try {
            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            // Get game stats to use team_elo instead of current_elo
            val gameStats = gameStatsRepository.findByGameId(game.gameId)
            val homeGameStats = gameStats.find { it.team == game.homeTeam }
            val awayGameStats = gameStats.find { it.team == game.awayTeam }

            // Use team_elo from game stats if available, otherwise fall back to current_elo
            val homeElo = homeGameStats?.teamElo ?: homeTeam.currentElo
            val awayElo = awayGameStats?.teamElo ?: awayTeam.currentElo
            winProbabilityService.calculateWinProbability(game, play, homeElo, awayElo)
        } catch (e: Exception) {
            Logger.error("Error calculating win probability: ${e.message}")
            play.winProbability = 0.5
            play.winProbabilityAdded = 0.0
        }

        // Update everything else
        game.homeScore = homeScore
        game.awayScore = awayScore
        game.winProbability = play.winProbability
        game.numPlays = play.playNumber
        game.gameTimer = calculateDelayOfGameTimer()
        game.gameWarning = GameWarning.NONE

        gameRepository.save(game)

        if (game.gameStatus == GameStatus.FINAL) {
            endGame(game)
        }

        return game
    }

    /**
     * Update the game information based on a defensive number submission
     * @param game
     * @param gamePlay
     */
    fun updateWithDefensiveNumberSubmission(
        game: Game,
        gamePlay: Play,
    ) {
        game.currentPlayId = gamePlay.playId
        game.waitingOn = game.possession
        game.gameTimer = calculateDelayOfGameTimer()
        gameRepository.save(game)
    }

    /**
     * Rollback the play to the previous play
     * @param game
     * @param previousPlay
     * @param gamePlay
     */
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
            if (isOffensiveTouchdownPlay(gamePlay.actualResult)) {
                if (gamePlay.possession == TeamSide.HOME) {
                    game.homeScore -= 6
                } else {
                    game.awayScore -= 6
                }
                game.currentPlayType = PlayType.NORMAL
            }
            if (isDefensiveTouchdownPlay(gamePlay.actualResult)) {
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
            Logger.error(
                "Error rolling back play.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Error Message: ${e.message!!}",
            )
            throw e
        }
    }

    /**
     * Start all games for the given week
     * @param season
     * @param week
     * @return
     */
    suspend fun startWeek(
        season: Int,
        week: Int,
    ): List<Game> {
        val gamesToStart =
            scheduleService.getGamesToStartBySeasonAndWeek(season, week) ?: run {
                Logger.error("No games found for season $season week $week")
                throw NoGameFoundException()
            }
        val startedGames = mutableListOf<Game>()
        var count = 0
        for (game in gamesToStart) {
            try {
                if (count >= 25) {
                    withContext(Dispatchers.IO) {
                        sleep(300000)
                    }
                    count = 0
                    Logger.info("Block of 25 games started, sleeping for 5 minutes")
                }
                val startedGame =
                    startNormalGame(
                        StartRequest(
                            DISCORD,
                            DISCORD,
                            game.subdivision,
                            game.homeTeam,
                            game.awayTeam,
                            game.tvChannel,
                            game.gameType,
                        ),
                        week,
                    )
                startedGames.add(startedGame)
                scheduleService.markGameAsStarted(game)
                count += 1
            } catch (e: Exception) {
                Logger.error(
                    "Error starting game in start week command.\n" +
                        "Home Team: ${game.homeTeam}\n" +
                        "Away Team: ${game.awayTeam}\n" +
                        "Error Message: ${e.message!!}",
                )
                continue
            }
        }
        return startedGames
    }

    /**
     * End all ongoing games
     */
    fun endAllGames(): List<Game> {
        val gamesToEnd = getAllOngoingGames()
        val endedGames = mutableListOf<Game>()
        for (game in gamesToEnd) {
            endedGames.add(endGame(game))
        }
        return endedGames
    }

    fun endDOGOutGame(
        game: Game,
        delayOfGameInstances: Pair<Int, Int>,
    ): Game {
        // If the home team has 3 delay of game instances, they lose,
        // so increment the score by 8 until they are losing
        if (delayOfGameInstances.first >= 3) {
            while (game.homeScore >= game.awayScore) {
                game.awayScore += 8
            }
        } else if (delayOfGameInstances.second >= 3) {
            while (game.awayScore >= game.homeScore) {
                game.homeScore += 8
            }
        }
        val updatedGame = saveGame(game)
        endGame(updatedGame)
        return game
    }

    /**
     * End a single game by channel id
     * @param channelId
     * @return
     */
    fun endSingleGameByChannelId(channelId: ULong): Game {
        val game = getGameByPlatformId(channelId)
        return endGame(game)
    }

    /**
     * End a single game by game id
     * @param gameId
     * @return
     */
    fun endSingleGameByGameId(gameId: Int): Game {
        val game = getGameById(gameId)
        return endGame(game)
    }

    /**
     * End a game
     * @param game
     * @return
     */
    private fun endGame(game: Game): Game {
        try {
            game.gameStatus = GameStatus.FINAL
            if (game.gameType != GameType.SCRIMMAGE) {
                teamService.updateTeamWinsAndLosses(game)
                userService.updateUserWinsAndLosses(game)

                // Update ELO ratings
                try {
                    val homeTeam = teamService.getTeamByName(game.homeTeam)
                    val awayTeam = teamService.getTeamByName(game.awayTeam)
                    winProbabilityService.updateEloRatings(game, homeTeam, awayTeam)
                    teamService.updateTeam(homeTeam)
                    teamService.updateTeam(awayTeam)
                } catch (e: Exception) {
                    Logger.error("Error updating ELO ratings: ${e.message}")
                }

                val homeUsers =
                    try {
                        val userList = mutableListOf<User>()
                        for (coach in game.homeCoachDiscordIds!!) {
                            userList.add(userService.getUserByDiscordId(coach))
                        }
                        userList
                    } catch (e: Exception) {
                        emptyList()
                    }
                val awayUsers =
                    try {
                        val userList = mutableListOf<User>()
                        for (coach in game.awayCoachDiscordIds!!) {
                            userList.add(userService.getUserByDiscordId(coach))
                        }
                        userList
                    } catch (e: Exception) {
                        emptyList()
                    }
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
            saveGame(game)

            val homeStats = gameStatsService.getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
            val awayStats = gameStatsService.getGameStatsByIdAndTeam(game.gameId, game.awayTeam)
            homeStats.gameStatus = GameStatus.FINAL
            awayStats.gameStatus = GameStatus.FINAL
            gameStatsService.saveGameStats(homeStats)
            gameStatsService.saveGameStats(awayStats)

            // Check if any records were broken
            recordService.checkAndUpdateRecordsForGame(game)

            // Update season stats for non-scrimmage games
            if (game.gameType != GameType.SCRIMMAGE) {
                seasonStatsService.updateSeasonStatsForGame(homeStats)
                seasonStatsService.updateSeasonStatsForGame(awayStats)
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
                    "Error Message: ${e.message!!}",
            )
            throw e
        }
    }

    /**
     * End the overtime period and advance to the next one
     * @param game
     */
    private fun endOvertimePeriod(game: Game) {
        game.overtimeHalf = 1
        game.possession =
            if (game.possession == TeamSide.HOME) {
                TeamSide.HOME
            } else {
                TeamSide.AWAY
            }
        game.ballLocation = 75
        game.down = 1
        game.yardsToGo = 10
        game.quarter += 1
        game.homeTimeouts = 1
        game.awayTimeouts = 1
        game.waitingOn = if (game.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
    }

    /**
     * Chew a game
     * @param channelId
     * @return
     */
    fun chewGame(game: Game): Game {
        try {
            game.gameMode = GameMode.CHEW
            saveGame(game)
            Logger.info(
                "Game set to chew mode.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Game Type: ${game.gameType}\n" +
                    "Game Status: ${game.gameStatus}\n" +
                    "Home team: ${game.homeTeam}\n" +
                    "Away team: ${game.awayTeam}\n",
            )
            return game
        } catch (e: Exception) {
            Logger.error("Error in ${game.gameId}: " + e.message!!)
            throw e
        }
    }

    /**
     * Run a coin toss
     * @param gameId
     * @param coinTossCall
     * @return
     */
    fun runCoinToss(
        gameId: String,
        coinTossCall: CoinTossCall,
    ): Game {
        val game = getGameById(gameId.toInt())

        try {
            val result = Random().nextInt(2)
            val coinTossWinner =
                if (
                    (result == 1 && coinTossCall == CoinTossCall.HEADS) ||
                    (result == 0 && coinTossCall == CoinTossCall.TAILS)
                ) {
                    TeamSide.AWAY
                } else {
                    TeamSide.HOME
                }
            if (game.gameStatus == GameStatus.PREGAME) {
                game.coinTossWinner = coinTossWinner
            } else if (game.gameStatus == GameStatus.END_OF_REGULATION) {
                game.overtimeCoinTossWinner = coinTossWinner
            }
            game.gameTimer = calculateDelayOfGameTimer()
            Logger.info(
                "Coin toss finished.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Coin Toss Winner: $coinTossWinner",
            )
            saveGame(game)
            return game
        } catch (e: Exception) {
            Logger.error(
                "Coin toss error.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Error Message: ${e.message!!}",
            )
            throw e
        }
    }

    /**
     * Make a coin toss choice
     * @param gameId
     * @param coinTossChoice
     * @return
     */
    fun makeCoinTossChoice(
        gameId: String,
        coinTossChoice: CoinTossChoice,
    ): Game {
        val game = getGameById(gameId.toInt())

        try {
            game.coinTossChoice = coinTossChoice
            if (game.coinTossWinner == TeamSide.HOME && coinTossChoice == CoinTossChoice.RECEIVE) {
                game.possession = TeamSide.AWAY
                game.waitingOn = TeamSide.HOME
            } else if (game.coinTossWinner == TeamSide.HOME && coinTossChoice == CoinTossChoice.DEFER) {
                game.possession = TeamSide.HOME
                game.waitingOn = TeamSide.AWAY
            } else if (game.coinTossWinner == TeamSide.AWAY && coinTossChoice == CoinTossChoice.RECEIVE) {
                game.possession = TeamSide.HOME
                game.waitingOn = TeamSide.AWAY
            } else if (game.coinTossWinner == TeamSide.AWAY && coinTossChoice == CoinTossChoice.DEFER) {
                game.possession = TeamSide.AWAY
                game.waitingOn = TeamSide.HOME
            }
            game.gameStatus = GameStatus.OPENING_KICKOFF
            game.gameTimer = calculateDelayOfGameTimer()
            Logger.info(
                "Coin toss choice made.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Coin Toss Winner: ${game.coinTossWinner}\n" +
                    "Coin Toss Choice: $coinTossChoice",
            )
            return saveGame(game)
        } catch (e: Exception) {
            Logger.error("Error in ${game.gameId}: " + e.message!!)
            if (e is IllegalArgumentException) {
                throw InvalidCoinTossChoiceException("Invalid coin toss choice: $coinTossChoice")
            }
            throw e
        }
    }

    /**
     * Make an overtime coin toss choice
     * @param gameId
     * @param coinTossChoice
     * @return
     */
    fun makeOvertimeCoinTossChoice(
        gameId: String,
        coinTossChoice: OvertimeCoinTossChoice,
    ): Game {
        val game = getGameById(gameId.toInt())

        try {
            game.overtimeCoinTossChoice = coinTossChoice
            if (game.overtimeCoinTossWinner == TeamSide.HOME && coinTossChoice == OvertimeCoinTossChoice.DEFENSE) {
                game.possession = TeamSide.AWAY
                game.waitingOn = TeamSide.HOME
            } else if (game.overtimeCoinTossWinner == TeamSide.HOME && coinTossChoice == OvertimeCoinTossChoice.OFFENSE) {
                game.possession = TeamSide.HOME
                game.waitingOn = TeamSide.AWAY
            } else if (game.overtimeCoinTossWinner == TeamSide.AWAY && coinTossChoice == OvertimeCoinTossChoice.DEFENSE) {
                game.possession = TeamSide.HOME
                game.waitingOn = TeamSide.AWAY
            } else if (game.overtimeCoinTossWinner == TeamSide.AWAY && coinTossChoice == OvertimeCoinTossChoice.OFFENSE) {
                game.possession = TeamSide.AWAY
                game.waitingOn = TeamSide.HOME
            }
            game.gameStatus = GameStatus.OVERTIME
            Logger.info(
                "Overtime coin toss choice made.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Coin Toss Winner: ${game.overtimeCoinTossWinner}\n" +
                    "Overtime Coin Toss Choice: $coinTossChoice",
            )
            return saveGame(game)
        } catch (e: Exception) {
            Logger.error(
                "Overtime coin toss choice error.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Error Message: ${e.message!!}",
            )
            if (e is IllegalArgumentException) {
                throw InvalidCoinTossChoiceException("Invalid coin toss choice $coinTossChoice")
            }
            throw e
        }
    }

    /**
     * Restart a game
     * @param channelId
     * @return
     */
    suspend fun restartGame(channelId: ULong): Game {
        val game = getGameByPlatformId(channelId)
        deleteOngoingGame(channelId)
        val startRequest =
            StartRequest(
                DISCORD,
                DISCORD,
                game.subdivision ?: Subdivision.FCFB,
                game.homeTeam,
                game.awayTeam,
                game.tvChannel,
                game.gameType ?: GameType.SCRIMMAGE,
            )
        return startNormalGame(startRequest, game.week)
    }

    /**
     * Deletes an ongoing game
     * @param channelId
     * @return
     */
    fun deleteOngoingGame(channelId: ULong): Boolean {
        val game = getGameByPlatformId(channelId)
        val id = game.gameId
        gameRepository.deleteById(id)
        gameStatsService.deleteByGameId(id)
        playRepository.deleteAllPlaysByGameId(id)
        Logger.info(
            "Game deleted.\n" +
                "Game ID: $id\n" +
                "Channel ID: $channelId",
        )
        return true
    }

    /**
     * Calculate the delay of game timer
     */
    fun calculateDelayOfGameTimer(): String? {
        // Set the DOG timer
        // Get the current date and time
        val now = ZonedDateTime.now(ZoneId.of("America/New_York"))

        // Add 18 hours to the current date and time
        val futureTime = now.plusHours(18)

        // Define the desired date and time format
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")

        // Format the result and set it on the game
        return futureTime.format(formatter)
    }

    /**
     * Update the request message id
     * @param gameId
     * @param requestMessageId
     */
    fun updateRequestMessageId(
        gameId: Int,
        requestMessageId: String,
    ): Game {
        val game = getGameById(gameId)

        val requestMessageIdList =
            if (requestMessageId.contains(",")) {
                listOf(requestMessageId.split(",")[0], requestMessageId.split(",")[1])
            } else {
                listOf(requestMessageId)
            }
        game.requestMessageId = requestMessageIdList
        saveGame(game)
        return game
    }

    /**
     * Update the last message timestamp
     * @param gameId
     */
    fun updateLastMessageTimestamp(gameId: Int): Game {
        val game = getGameById(gameId)
        val timestamp = Timestamp.from(Instant.now()).toString()
        game.lastMessageTimestamp = timestamp
        saveGame(game)
        return game
    }

    /**
     * Update a game
     * @param game
     */
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
        return existingGame
    }

    /**
     * Update the timeouts for a game
     */
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

    /**
     * Update the team the game is waiting on
     */
    private fun updateWaitingOn(possession: TeamSide): TeamSide {
        return if (possession == TeamSide.HOME) {
            TeamSide.AWAY
        } else {
            TeamSide.HOME
        }
    }

    /**
     * Update the play type for a game
     */
    private fun updatePlayType(
        game: Game,
        play: Play,
    ) {
        if (isTouchdownPlay(play.actualResult)) {
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

    /**
     * Update the values to start a game
     * @param game
     * @param clock
     * @param possession
     * @param quarter
     * @param ballLocation
     * @param down
     * @param yardsToGo
     * @param waitingOn
     */
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

    /**
     * Update the values for when a game is going into halftime
     * @param game
     * @param possession
     * @param waitingOn
     * @param clock
     */
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

    /**
     * Update the values for the start of a half
     * @param game
     * @param clock
     * @param possession
     * @param quarter
     * @param ballLocation
     * @param down
     * @param yardsToGo
     * @param waitingOn
     */
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

    /**
     * Update the end of game values for regulation games
     * @param game
     */
    private fun updateEndOfRegulationGameValues(game: Game) {
        game.quarter = 4
        game.clock = "0:00"
        game.clockStopped = true
        game.gameStatus = GameStatus.FINAL
    }

    /**
     * Update the values for a normal play
     * @param game
     * @param clock
     * @param possession
     * @param quarter
     * @param ballLocation
     * @param down
     * @param yardsToGo
     * @param waitingOn
     */
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

    /**
     * Update the values for the start of overtime
     * @param game
     * @param quarter
     */
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

    /**
     * Update overtime values for a game
     * @param game
     * @param play
     * @param possession
     * @param waitingOn
     * @param ballLocation
     * @param down
     * @param yardsToGo
     * @param homeScore
     * @param awayScore
     */
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
        if (isEndOfOvertimeHalf(play)) {
            // Handle the end of each half of overtime
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
                    // End of game, one team has won
                    // If the game is within 2 points, kick the PAT
                    if (isGameMathmaticallyOver(play, homeScore, awayScore)) {
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

    /**
     * Sub a coach in for a team
     * @param gameId
     */
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

    /**
     * Get the game by request message id
     * @param requestMessageId
     */
    fun getGameByRequestMessageId(requestMessageId: String) =
        gameRepository.getGameByRequestMessageId(requestMessageId)
            ?: throw GameNotFoundException("Game not found for Request Message ID: $requestMessageId")

    /**
     * Get the game by platform id
     * @param platformId
     */
    fun getGameByPlatformId(platformId: ULong) =
        gameRepository.getGameByPlatformId(platformId)
            ?: throw GameNotFoundException("Game not found for Platform ID: $platformId")

    /**
     * Get an ongoing game by id
     * @param id
     * @return
     */
    fun getGameById(id: Int): Game = gameRepository.getGameById(id) ?: throw GameNotFoundException("No game found with ID: $id")

    /**
     * Get filtered games
     * @param filters
     * @param category
     * @param conference
     * @param season
     * @param week
     * @param gameMode
     * @param sort
     * @param pageable
     */
    fun getFilteredGames(
        filters: List<GameFilter>,
        category: GameCategory?,
        conference: String?,
        season: Int?,
        week: Int?,
        gameMode: GameMode?,
        sort: GameSort,
        pageable: Pageable,
    ): Page<Game> {
        val filterSpec = gameSpecificationService.createSpecification(filters, category, conference, season, week, gameMode)
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

    /**
     * Find expired timers
     */
    fun findExpiredTimers() =
        gameRepository.findExpiredTimers().ifEmpty {
            Logger.info("No games found with expired timers")
            emptyList()
        }

    /**
     * Find games to warn first instance
     */
    fun findGamesToWarnFirstInstance() =
        gameRepository.findGamesToWarnFirstInstance().ifEmpty {
            emptyList()
        }

    /**
     * Find games to warn second instance
     */
    fun findGamesToWarnSecondInstance() =
        gameRepository.findGamesToWarnSecondInstance().ifEmpty {
            emptyList()
        }

    /**
     * Update a game as warned
     * @param gameId
     */
    fun updateGameAsWarned(
        gameId: Int,
        instance: Int,
    ) = if (instance == 1) {
        gameRepository.updateGameAsFirstWarning(gameId)
    } else {
        gameRepository.updateGameAsSecondWarning(gameId)
    }

    /**
     * Mark a game as close game pinged
     * @param gameId
     */
    fun markCloseGamePinged(gameId: Int) = gameRepository.markCloseGamePinged(gameId)

    /**
     * Mark a game as upset alert pinged
     * @param gameId
     */
    fun markUpsetAlertPinged(gameId: Int) = gameRepository.markUpsetAlertPinged(gameId)

    /**
     * Get all games
     */
    fun getAllGames() =
        gameRepository.getAllGames().ifEmpty {
            throw GameNotFoundException("No games found when getting all games")
        }

    /**
     * Get all ongoing games
     */
    private fun getAllOngoingGames() =
        gameRepository.getAllOngoingGames().ifEmpty {
            throw GameNotFoundException("No ongoing games found")
        }

    /**
     * Get all games with the teams in it for the requested week
     * @param teams
     * @param season
     * @param week
     */
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

    /**
     * Get games by season and matchup
     */
    fun getGameBySeasonAndMatchup(
        season: Int,
        firstTeam: String,
        secondTeam: String,
    ) = gameRepository.getGamesBySeasonAndMatchup(season, firstTeam, secondTeam)

    /**
     * Handle the halftime possession change
     * @param game
     * @return
     */
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

    /**
     * Determines if the game is close
     * @param game the game
     * @param play the play
     */
    private fun updateCloseGame(
        game: Game,
        play: Play,
    ) {
        game.closeGame = abs(game.homeScore - game.awayScore) <= 8 &&
            play.quarter >= 4 &&
            play.clock <= 210
    }

    /**
     * Determine if there is an upset alert. A game is an upset alert
     * if at least one of the teams is ranked and the game is close
     * @param game the game
     * @param play the play
     */
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

    /**
     * Returns the difference between the offensive and defensive numbers.
     * @param offensiveNumber
     * @param defesiveNumber
     * @return
     */
    fun getDifference(
        offensiveNumber: Int,
        defesiveNumber: Int,
    ): Int {
        var difference = abs(defesiveNumber - offensiveNumber)
        if (difference > 750) {
            difference = 1500 - difference
        }
        return difference
    }

    /**
     * Returns the number of seconds from the clock.
     * @param clock
     * @return
     */
    fun convertClockToSeconds(clock: String): Int {
        val clockArray = clock.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val minutes = clockArray[0].toInt()
        val seconds = clockArray[1].toInt()
        return minutes * 60 + seconds
    }

    /**
     * Returns the clock from the number of seconds.
     * @param seconds
     * @return
     */
    private fun convertClockToString(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    /**
     * Check if a play is a touchdown play
     * @param actualResult
     * @return
     */
    fun isTouchdownPlay(actualResult: ActualResult?): Boolean {
        return actualResult == ActualResult.TOUCHDOWN ||
            actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
            actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN ||
            actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
            actualResult == ActualResult.RETURN_TOUCHDOWN ||
            actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
            actualResult == ActualResult.KICK_SIX
    }

    /**
     * Check if a play is an offensive scoring play
     * @param actualResult
     * @return
     */
    private fun isOffensiveTouchdownPlay(actualResult: ActualResult?): Boolean {
        return actualResult == ActualResult.TOUCHDOWN ||
            actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
            actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN
    }

    /**
     * Check if a play is an defensive scoring play
     * @param actualResult
     * @return
     */
    private fun isDefensiveTouchdownPlay(actualResult: ActualResult?): Boolean {
        return actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
            actualResult == ActualResult.RETURN_TOUCHDOWN ||
            actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
            actualResult == ActualResult.KICK_SIX
    }

    /**
     * Determine if the clock is stopped
     */
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

    private fun isEndOfOvertimeHalf(play: Play) =
        play.actualResult == ActualResult.GOOD ||
            play.actualResult == ActualResult.NO_GOOD ||
            play.actualResult == ActualResult.BLOCKED ||
            play.actualResult == ActualResult.SUCCESS ||
            play.actualResult == ActualResult.FAILED ||
            play.actualResult == ActualResult.DEFENSE_TWO_POINT ||
            play.actualResult == ActualResult.TURNOVER_ON_DOWNS ||
            play.actualResult == ActualResult.TURNOVER ||
            play.actualResult == ActualResult.TOUCHDOWN ||
            play.actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
            play.actualResult == ActualResult.KICK_SIX ||
            play.actualResult == ActualResult.PUNT ||
            play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
            play.actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN ||
            play.actualResult == ActualResult.MUFFED_PUNT

    /**
     * Check if the game is mathematically over
     * @param play
     * @param homeScore
     * @param awayScore
     */
    private fun isGameMathmaticallyOver(
        play: Play,
        homeScore: Int,
        awayScore: Int,
    ) = (
        play.actualResult == ActualResult.TOUCHDOWN ||
            play.actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
            play.actualResult == ActualResult.KICK_SIX
    ) && (abs(homeScore - awayScore) <= 2 || abs(awayScore - homeScore) <= 2)

    /**
     * Create a Discord thread for the game and get the data from it
     * @param game
     * @return
     */
    private suspend fun createDiscordThread(game: Game): List<String> {
        val discordData =
            discordService.createGameThread(game)
                ?: run {
                    deleteOngoingGame(
                        game.homePlatformId?.toULong() ?: game.awayPlatformId?.toULong()
                            ?: throw UnableToDeleteGameException(),
                    )
                    throw UnableToCreateGameThreadException()
                }

        if (discordData[0] == "null") {
            deleteOngoingGame(
                game.homePlatformId?.toULong() ?: game.awayPlatformId?.toULong()
                    ?: throw UnableToDeleteGameException(),
            )
            throw UnableToCreateGameThreadException()
        }
        return discordData
    }

    /**
     * Get the current season and week
     * @param startRequest
     * @param week
     */
    private fun getCurrentSeasonAndWeek(
        startRequest: StartRequest,
        week: Int?,
    ): Pair<Int?, Int?> {
        var (season, currentWeek) =
            if (startRequest.gameType != GameType.SCRIMMAGE) {
                seasonService.getCurrentSeason().seasonNumber to seasonService.getCurrentSeason().currentWeek
            } else {
                null to null
            }

        if (week != null) {
            currentWeek = week
        }
        return season to currentWeek
    }
}
