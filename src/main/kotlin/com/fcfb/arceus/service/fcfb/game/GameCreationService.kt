package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.system.Platform.DISCORD
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.discord.UnableToCreateGameThreadException
import com.fcfb.arceus.exception.team.NoCoachDiscordIdsFoundException
import com.fcfb.arceus.exception.team.NoCoachesFoundException
import com.fcfb.arceus.exception.team.TeamNotFoundException
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.appearance.TeamUniformService
import com.fcfb.arceus.service.fcfb.probability.VegasOddsService
import com.fcfb.arceus.service.fcfb.ranking.RankingService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import com.fcfb.arceus.service.fcfb.season.SeasonService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GameCreationService(
    private val gameService: GameService,
    private val gameRepository: GameRepository,
    private val teamService: TeamService,
    private val discordService: DiscordService,
    private val gameStatsService: GameStatsService,
    private val teamUniformService: TeamUniformService,
    private val seasonService: SeasonService,
    private val scheduleService: ScheduleService,
    private val vegasOddsService: VegasOddsService,
    private val rankingService: RankingService,
) {
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

    internal suspend fun startNormalGame(
        startRequest: StartRequest,
        week: Int?,
    ): Game {
        try {
            val homeTeamData = teamService.getTeamByName(startRequest.homeTeam)
            val awayTeamData = teamService.getTeamByName(startRequest.awayTeam)

            val formattedDateTime = gameService.calculateDelayOfGameTimer()

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
            val (homeTeamRank, awayTeamRank) = getTeamRanks(season, currentWeek, homeTeamData.id, awayTeamData.id)

            val vegasOdds = vegasOddsService.calculateVegasOdds(homeTeamData, awayTeamData)

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
                            postseasonGameLogo = startRequest.postseasonGameLogo,
                            postseasonGameName = startRequest.postseasonGameName,
                            neutralSite = startRequest.neutralSite,
                            venue = startRequest.venue,
                        ),
                    )
                }

            completeGameCreation(newGame)

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
                    "Error Message: ${e.message ?: "Unknown error"}\n" +
                    "Game Type: ${startRequest.gameType}\n" +
                    "Home team: ${startRequest.homeTeam}\n" +
                    "Away team: ${startRequest.awayTeam}",
            )
            throw e
        }
    }

    suspend fun startOvertimeGame(startRequest: StartRequest): Game {
        try {
            val homeTeamData = teamService.getTeamByName(startRequest.homeTeam)
            val awayTeamData = teamService.getTeamByName(startRequest.awayTeam)

            val formattedDateTime = gameService.calculateDelayOfGameTimer()

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

            val (homeTeamRank, awayTeamRank) = getTeamRanks(null, null, homeTeamData.id, awayTeamData.id)

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

            completeGameCreation(newGame)

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
                    "Error Message: ${e.message ?: "Unknown error"}\n" +
                    "Game Type: ${startRequest.gameType}\n" +
                    "Home team: ${startRequest.homeTeam}\n" +
                    "Away team: ${startRequest.awayTeam}",
            )
            throw e
        }
    }

    suspend fun restartGame(channelId: String): Game {
        val platformId = channelId.toULong()
        val game = gameService.getGameByPlatformId(platformId)
        gameService.deleteOngoingGame(platformId)
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

    private suspend fun createDiscordThread(game: Game): List<String> {
        val discordData = discordService.createGameThread(game)

        if (discordData == null || discordData.size < 2 || discordData[0] == "null" || discordData[0].isBlank()) {
            gameService.deleteGameById(game.gameId)
            throw UnableToCreateGameThreadException()
        }
        return discordData
    }

    private fun getTeamRanks(
        season: Int?,
        week: Int?,
        homeTeamId: Int,
        awayTeamId: Int,
    ): Pair<Int?, Int?> {
        val currentSeason = seasonService.getCurrentSeason()
        return rankingService.getTeamRanks(
            season ?: currentSeason.seasonNumber,
            week ?: currentSeason.currentWeek,
            homeTeamId,
            awayTeamId,
        )
    }

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

    private suspend fun completeGameCreation(game: Game) {
        val discordData = createDiscordThread(game)
        game.homePlatformId = discordData[0]
        game.awayPlatformId = discordData[0]
        game.requestMessageId = listOf(discordData[1])
        withContext(Dispatchers.IO) {
            gameRepository.save(game)
        }
        gameStatsService.createGameStats(game)
        teamUniformService.snapshot(game)
    }
}
