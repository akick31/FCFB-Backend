package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.domain.elo.EloCalculator
import com.fcfb.arceus.domain.statistics.GameStatsCalculator
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.exception.statistics.GameStatsNotFoundException
import com.fcfb.arceus.exception.statistics.InvalidGameStatsRequestException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.statistics.elo.EloService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.statistics.StatsAverageCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class GameStatsService(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val teamRepository: TeamRepository,
    private val eloService: EloService,
) {
    private val eloCalculator = EloCalculator()
    private val statsAverageCalculator = StatsAverageCalculator()
    private val gameStatsCalculator = GameStatsCalculator(statsAverageCalculator)

    fun createGameStats(game: Game): List<GameStats> {
        val homeTeam =
            teamRepository.getTeamByName(game.homeTeam)
                ?: throw Exception("Could not find home team: ${game.homeTeam}")
        val awayTeam =
            teamRepository.getTeamByName(game.awayTeam)
                ?: throw Exception("Could not find away team: ${game.awayTeam}")

        val homeStats =
            GameStats(
                gameId = game.gameId,
                team = game.homeTeam,
                tvChannel = game.tvChannel,
                coaches = game.homeCoaches,
                coachDiscordIds = game.homeCoachDiscordIds,
                offensivePlaybook = game.homeOffensivePlaybook,
                defensivePlaybook = game.homeDefensivePlaybook,
                season = game.season,
                week = game.week,
                subdivision = game.subdivision,
                gameStatus = game.gameStatus,
                gameType = game.gameType,
                teamElo = homeTeam.currentElo,
            )
        gameStatsRepository.save(homeStats) ?: throw Exception("Could not create game stats for home team")

        val awayStats =
            GameStats(
                gameId = game.gameId,
                team = game.awayTeam,
                tvChannel = game.tvChannel,
                coaches = game.awayCoaches,
                coachDiscordIds = game.awayCoachDiscordIds,
                offensivePlaybook = game.awayOffensivePlaybook,
                defensivePlaybook = game.awayDefensivePlaybook,
                season = game.season,
                week = game.week,
                subdivision = game.subdivision,
                gameStatus = game.gameStatus,
                gameType = game.gameType,
                teamElo = awayTeam.currentElo,
            )
        gameStatsRepository.save(awayStats) ?: throw Exception("Could not create game stats for away team")

        return listOf(homeStats, awayStats)
    }

    private fun buildUnsavedGameStats(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ): Pair<GameStats, GameStats> {
        val homeStats =
            GameStats(
                gameId = game.gameId,
                team = game.homeTeam,
                tvChannel = game.tvChannel,
                coaches = game.homeCoaches,
                coachDiscordIds = game.homeCoachDiscordIds,
                offensivePlaybook = game.homeOffensivePlaybook,
                defensivePlaybook = game.homeDefensivePlaybook,
                season = game.season,
                week = game.week,
                subdivision = game.subdivision,
                gameStatus = game.gameStatus,
                gameType = game.gameType,
                teamElo = homeTeam.currentElo,
            )

        val awayStats =
            GameStats(
                gameId = game.gameId,
                team = game.awayTeam,
                tvChannel = game.tvChannel,
                coaches = game.awayCoaches,
                coachDiscordIds = game.awayCoachDiscordIds,
                offensivePlaybook = game.awayOffensivePlaybook,
                defensivePlaybook = game.awayDefensivePlaybook,
                season = game.season,
                week = game.week,
                subdivision = game.subdivision,
                gameStatus = game.gameStatus,
                gameType = game.gameType,
                teamElo = awayTeam.currentElo,
            )

        return homeStats to awayStats
    }

    fun generateGameStatsForGamesMoreRecentThanGameId(gameId: Int) {
        try {
            val allGames =
                gameRepository.getAllGamesMoreRecentThanGameId(gameId).ifEmpty {
                    throw GameNotFoundException("Could not find any games more recent than game ID $gameId")
                }

            for (game in allGames) {
                Logger().info("Generating game stats for game ${game.gameId}")
                generateGameStats(game.gameId)
            }
        } catch (e: Exception) {
            throw Exception("Could not generate game stats for games more recent than game ID $gameId", e)
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun generateAllGameStats() {
        try {
            val eloRebuild = eloService.rebuild(apply = true)
            Logger().info(
                "Rebuilt ELO from scratch: ${eloRebuild.teamsChanged.size} teams, " +
                    "${eloRebuild.gameSpreadsChanged} game spreads, ${eloRebuild.gameStatsRowsChanged} game stats rows updated",
            )
            val allGames =
                gameRepository.getAllGames().ifEmpty {
                    throw GameNotFoundException("Could not find any games")
                }

            Logger().info("Starting generation of all game stats for ${allGames.size} games")

            val teamsByName = teamRepository.findAll().associateBy { it.name }
            val allStats = mutableListOf<GameStats>()

            val batchSize = 200
            var processedGames = 0
            for (gameBatch in allGames.chunked(batchSize)) {
                val gameIds = gameBatch.map { it.gameId }
                val playsByGameId = playRepository.findByGameIdIn(gameIds).groupBy { it.gameId }

                for (game in gameBatch) {
                    val homeTeam =
                        teamsByName[game.homeTeam]
                            ?: throw Exception("Could not find home team: ${game.homeTeam}")
                    val awayTeam =
                        teamsByName[game.awayTeam]
                            ?: throw Exception("Could not find away team: ${game.awayTeam}")

                    val (homeStats, awayStats) = buildUnsavedGameStats(game, homeTeam, awayTeam)
                    val playedPlays =
                        (playsByGameId[game.gameId] ?: emptyList())
                            .filter { it.actualResult != ActualResult.END_OF_GAME }

                    updateScoreStats(playedPlays, homeStats, TeamSide.HOME)
                    allStats.add(updateStats(playedPlays, TeamSide.HOME, game, homeStats))

                    updateScoreStats(playedPlays, awayStats, TeamSide.AWAY)
                    allStats.add(updateStats(playedPlays, TeamSide.AWAY, game, awayStats))
                }

                processedGames += gameBatch.size
                Logger().info(
                    "Computed game stats through game ${gameBatch.last().gameId} " +
                        "($processedGames/${allGames.size} games)",
                )
            }

            Logger().info("Saving computed game stats for ${allGames.size} games")
            seedPreGameElo(allGames, allStats)
            gameStatsRepository.deleteByGameIdIn(allGames.map { it.gameId })
            gameStatsRepository.saveAll(allStats)
            Logger().info("Completed generation of all game stats")
        } catch (e: Exception) {
            throw Exception("Could not generate game stats", e)
        }
    }

    private fun seedPreGameElo(
        games: List<Game>,
        stats: List<GameStats>,
    ) {
        val preGameElo = eloCalculator.replay(games).preGameElo
        stats.forEach { row -> row.team?.let { team -> preGameElo[row.gameId to team]?.let { row.teamElo = it } } }
    }

    fun generateGameStats(gameId: Int) {
        val existingEloByTeam = gameStatsRepository.findByGameId(gameId).associate { it.team to it.teamElo }

        deleteByGameId(gameId)

        val game =
            gameRepository.getGameById(gameId)
                ?: throw Exception("Could not find game with ID $gameId")
        val newStats = createGameStats(game)

        if (existingEloByTeam.isNotEmpty()) {
            newStats.forEach { stats ->
                existingEloByTeam[stats.team]?.let { stats.teamElo = it }
                gameStatsRepository.save(stats)
            }
        }

        val allPlays = playRepository.getAllPlaysByGameId(gameId)

        updateGameStats(game, allPlays)
    }

    fun updateGameStats(
        game: Game,
        allPlays: List<Play>,
    ): List<GameStats> {
        val playedPlays = allPlays.filter { it.actualResult != ActualResult.END_OF_GAME }
        var homeStats = getOrCreateGameStatsByIdAndTeam(game, game.homeTeam)
        updateScoreStats(playedPlays, homeStats, TeamSide.HOME)
        homeStats = updateStats(playedPlays, TeamSide.HOME, game, homeStats)
        saveGameStats(homeStats)

        var awayStats = getOrCreateGameStatsByIdAndTeam(game, game.awayTeam)
        updateScoreStats(playedPlays, awayStats, TeamSide.AWAY)
        awayStats = updateStats(playedPlays, TeamSide.AWAY, game, awayStats)
        saveGameStats(awayStats)
        return listOf(homeStats, awayStats)
    }

    fun saveGameStats(gameStats: GameStats) = gameStatsRepository.save(gameStats)

    fun reconcileForfeitScore(
        game: Game,
        homeStats: GameStats,
        awayStats: GameStats,
    ) {
        val homeGap = game.homeScore - (homeStats.q1Score + homeStats.q2Score + homeStats.q3Score + homeStats.q4Score + homeStats.otScore)
        if (homeGap > 0) {
            homeStats.otScore += homeGap
            saveGameStats(homeStats)
        }
        val awayGap = game.awayScore - (awayStats.q1Score + awayStats.q2Score + awayStats.q3Score + awayStats.q4Score + awayStats.otScore)
        if (awayGap > 0) {
            awayStats.otScore += awayGap
            saveGameStats(awayStats)
        }
    }

    fun getGameStatsByIdAndTeam(
        gameId: Int,
        team: String,
    ) = gameStatsRepository.getGameStatsByIdAndTeam(gameId, team)
        ?: throw GameStatsNotFoundException("Could not find game stats for game $gameId and team $team")

    fun getGameStatsForGame(
        gameId: Int?,
        team: String,
    ): GameStats = getGameStatsByIdAndTeam(gameId ?: throw InvalidGameStatsRequestException("gameId parameter is required"), team)

    fun getGameStatsForTeamSeason(
        team: String,
        season: Int?,
    ): List<GameStats> =
        getAllGameStatsForTeamAndSeason(team, season ?: throw InvalidGameStatsRequestException("season parameter is required"))

    private fun getOrCreateGameStatsByIdAndTeam(
        game: Game,
        team: String,
    ): GameStats {
        gameStatsRepository.getGameStatsByIdAndTeam(game.gameId, team)?.let { return it }

        Logger().warn("Game stats missing for game ${game.gameId} team $team - creating baseline row")
        val isHome = team == game.homeTeam
        val teamModel =
            teamRepository.getTeamByName(team)
                ?: throw Exception("Could not find team: $team")
        val stats =
            GameStats(
                gameId = game.gameId,
                team = team,
                tvChannel = game.tvChannel,
                coaches = if (isHome) game.homeCoaches else game.awayCoaches,
                coachDiscordIds = if (isHome) game.homeCoachDiscordIds else game.awayCoachDiscordIds,
                offensivePlaybook = if (isHome) game.homeOffensivePlaybook else game.awayOffensivePlaybook,
                defensivePlaybook = if (isHome) game.homeDefensivePlaybook else game.awayDefensivePlaybook,
                season = game.season,
                week = game.week,
                subdivision = game.subdivision,
                gameStatus = game.gameStatus,
                gameType = game.gameType,
                teamElo = teamModel.currentElo,
            )
        return gameStatsRepository.save(stats)
    }

    fun getGameStatsById(gameId: Int) = gameStatsRepository.findByGameId(gameId)

    fun deleteByGameId(gameId: Int) = gameStatsRepository.deleteByGameId(gameId)

    private fun updateStats(
        allPlays: List<Play>,
        teamSide: TeamSide,
        game: Game,
        stats: GameStats,
    ): GameStats {
        val allOffensivePlays = allPlays.filter { play -> play.possession == teamSide }
        val allDefensivePlays = allPlays.filter { play -> play.possession != teamSide }

        stats.score = if (teamSide == TeamSide.HOME) game.homeScore else game.awayScore
        stats.passAttempts = gameStatsCalculator.calculatePassAttempts(allOffensivePlays)
        stats.passCompletions = gameStatsCalculator.calculatePassCompletions(allOffensivePlays)
        stats.passCompletionPercentage = gameStatsCalculator.calculatePercentage(stats.passCompletions, stats.passAttempts)
        stats.passYards = gameStatsCalculator.calculatePassYards(allOffensivePlays)
        stats.longestPass = gameStatsCalculator.calculateLongestPass(allOffensivePlays)
        stats.sacksAllowed = gameStatsCalculator.calculateSacksAllowed(allOffensivePlays)
        stats.sacksForced = gameStatsCalculator.calculateSacksAllowed(allDefensivePlays)
        stats.rushAttempts = gameStatsCalculator.calculateRushAttempts(allOffensivePlays)
        stats.rushSuccesses = gameStatsCalculator.calculateRushSuccesses(allOffensivePlays)
        stats.rushSuccessPercentage = gameStatsCalculator.calculatePercentage(stats.rushSuccesses, stats.rushAttempts)
        stats.passSuccesses = gameStatsCalculator.calculatePassSuccesses(allOffensivePlays)
        stats.passSuccessPercentage = gameStatsCalculator.calculatePercentage(stats.passSuccesses, stats.passAttempts)
        stats.rushYards = gameStatsCalculator.calculateRushYards(allOffensivePlays)
        stats.longestRun = gameStatsCalculator.calculateLongestRun(allOffensivePlays)
        stats.totalYards = gameStatsCalculator.calculateTotalYards(stats.passYards, stats.rushYards)
        stats.interceptionsLost = gameStatsCalculator.calculateInterceptionsLost(allOffensivePlays)
        stats.interceptionsForced = gameStatsCalculator.calculateInterceptionsLost(allDefensivePlays)
        stats.fumblesLost = gameStatsCalculator.calculateFumblesLost(allOffensivePlays)
        stats.fumblesForced = gameStatsCalculator.calculateFumblesLost(allDefensivePlays)
        stats.turnoversLost = gameStatsCalculator.calculateTurnoversLost(stats.interceptionsLost, stats.fumblesLost)
        stats.turnoversForced = gameStatsCalculator.calculateTurnoversLost(stats.interceptionsForced, stats.fumblesForced)
        stats.turnoverTouchdownsLost = gameStatsCalculator.calculateTurnoverTouchdownsLost(allOffensivePlays)
        stats.turnoverTouchdownsForced = gameStatsCalculator.calculateTurnoverTouchdownsLost(allDefensivePlays)
        stats.fieldGoalMade = gameStatsCalculator.calculateFieldGoalMade(allOffensivePlays)
        stats.fieldGoalAttempts = gameStatsCalculator.calculateFieldGoalAttempts(allOffensivePlays)
        stats.fieldGoalPercentage = gameStatsCalculator.calculatePercentage(stats.fieldGoalMade, stats.fieldGoalAttempts)
        stats.longestFieldGoal = gameStatsCalculator.calculateLongestFieldGoal(allOffensivePlays)
        stats.blockedOpponentFieldGoals = gameStatsCalculator.calculateBlockedOpponentFieldGoals(allDefensivePlays)
        stats.fieldGoalTouchdown = gameStatsCalculator.calculateFieldGoalTouchdown(allOffensivePlays)
        stats.puntsAttempted = gameStatsCalculator.calculatePuntsAttempted(allOffensivePlays)
        stats.longestPunt = gameStatsCalculator.calculateLongestPunt(allOffensivePlays)
        stats.puntYards = gameStatsCalculator.calculatePuntYards(allOffensivePlays)
        stats.puntCount = gameStatsCalculator.calculatePuntCount(allOffensivePlays)
        stats.averagePuntLength = statsAverageCalculator.average(stats.puntYards, stats.puntCount)
        stats.blockedOpponentPunt = gameStatsCalculator.calculateBlockedOpponentPunt(allOffensivePlays)
        stats.puntReturnTd = gameStatsCalculator.calculatePuntReturnTd(allOffensivePlays)
        stats.puntReturnTdPercentage =
            gameStatsCalculator.calculatePercentage(
                stats.puntReturnTd,
                gameStatsCalculator.calculatePuntsAttempted(allDefensivePlays),
            )
        stats.numberOfKickoffs = gameStatsCalculator.calculateNumberOfKickoffs(allDefensivePlays)
        stats.onsideAttempts = gameStatsCalculator.calculateOnsideAttempts(allOffensivePlays)
        stats.onsideSuccess = gameStatsCalculator.calculateOnsideSuccess(allOffensivePlays)
        stats.onsideSuccessPercentage = gameStatsCalculator.calculatePercentage(stats.onsideSuccess, stats.onsideAttempts)
        stats.normalKickoffAttempts = gameStatsCalculator.calculateNormalKickoffAttempts(allOffensivePlays)
        stats.touchbacks = gameStatsCalculator.calculateTouchbacks(allOffensivePlays)
        stats.touchbackPercentage =
            gameStatsCalculator.calculatePercentage(
                stats.touchbacks,
                stats.normalKickoffAttempts,
            )
        stats.kickReturnTd = gameStatsCalculator.calculateKickReturnTd(allDefensivePlays)
        stats.kickReturnTdPercentage = gameStatsCalculator.calculatePercentage(stats.kickReturnTd, stats.numberOfKickoffs)
        stats.numberOfDrives = gameStatsCalculator.calculateNumberOfDrives(allPlays, teamSide)
        stats.timeOfPossession = gameStatsCalculator.calculateTimeOfPossession(allOffensivePlays)
        stats.touchdowns = gameStatsCalculator.calculateTouchdowns(allPlays, teamSide)
        stats.averageOffensiveDiff = gameStatsCalculator.calculateAverageNormalPlayDiff(allOffensivePlays)
        stats.averageDefensiveDiff = gameStatsCalculator.calculateAverageNormalPlayDiff(allDefensivePlays)
        stats.averageOffensiveSpecialTeamsDiff = gameStatsCalculator.calculateAverageSpecialTeamsDiff(allOffensivePlays)
        stats.averageDefensiveSpecialTeamsDiff = gameStatsCalculator.calculateAverageSpecialTeamsDiff(allDefensivePlays)
        stats.offensivePlayYards = gameStatsCalculator.calculateOffensivePlayYards(allOffensivePlays)
        stats.offensivePlayCount = gameStatsCalculator.calculateOffensivePlayCount(allOffensivePlays)
        stats.averageYardsPerPlay = statsAverageCalculator.average(stats.offensivePlayYards, stats.offensivePlayCount)
        stats.firstDowns = gameStatsCalculator.calculateFirstDowns(allOffensivePlays)
        stats.thirdDownConversionSuccess = gameStatsCalculator.calculateThirdDownConversionSuccess(allOffensivePlays)
        stats.thirdDownConversionAttempts = gameStatsCalculator.calculateThirdDownConversionAttempts(allOffensivePlays)
        stats.thirdDownConversionPercentage =
            gameStatsCalculator.calculatePercentage(stats.thirdDownConversionSuccess, stats.thirdDownConversionAttempts)
        stats.fourthDownConversionSuccess = gameStatsCalculator.calculateFourthDownConversionSuccess(allOffensivePlays)
        stats.fourthDownConversionAttempts = gameStatsCalculator.calculateFourthDownConversionAttempts(allOffensivePlays)
        stats.fourthDownConversionPercentage =
            gameStatsCalculator.calculatePercentage(stats.fourthDownConversionSuccess, stats.fourthDownConversionAttempts)
        if (teamSide == TeamSide.HOME) {
            stats.largestLead = gameStatsCalculator.calculateLargestLeadForHome(allPlays)
            stats.largestDeficit = gameStatsCalculator.calculateLargestDeficitForHome(allPlays)
        } else {
            stats.largestLead = gameStatsCalculator.calculateLargestLeadForAway(allPlays)
            stats.largestDeficit = gameStatsCalculator.calculateLargestDeficitForAway(allPlays)
        }
        val teamSpread = if (teamSide == TeamSide.HOME) game.homeVegasSpread else game.awayVegasSpread
        val teamWon = if (teamSide == TeamSide.HOME) game.homeScore > game.awayScore else game.awayScore > game.homeScore
        stats.favoredMargin = gameStatsCalculator.calculateFavoredMargin(teamSpread)
        stats.upsetMargin = gameStatsCalculator.calculateUpsetMargin(teamSpread, teamWon)
        stats.passTouchdowns = gameStatsCalculator.calculatePassTouchdowns(allOffensivePlays)
        stats.rushTouchdowns = gameStatsCalculator.calculateRushTouchdowns(allOffensivePlays)
        stats.redZoneAttempts = gameStatsCalculator.calculateRedZoneAttempts(allPlays, teamSide)
        stats.redZoneSuccesses = gameStatsCalculator.calculateRedZoneSuccesses(allOffensivePlays)
        stats.redZoneSuccessPercentage = gameStatsCalculator.calculatePercentage(stats.redZoneSuccesses, stats.redZoneAttempts)
        stats.redZonePercentage = gameStatsCalculator.calculatePercentage(stats.redZoneAttempts, stats.numberOfDrives)
        stats.turnoverDifferential = gameStatsCalculator.calculateTurnoverDifferential(stats.turnoversLost, stats.turnoversForced)
        stats.pickSixesThrown = gameStatsCalculator.calculatePickSixes(allOffensivePlays)
        stats.pickSixesForced = gameStatsCalculator.calculatePickSixes(allDefensivePlays)
        stats.fumbleReturnTdsCommitted = gameStatsCalculator.calculateFumbleReturnTds(allOffensivePlays)
        stats.fumbleReturnTdsForced = gameStatsCalculator.calculateFumbleReturnTds(allDefensivePlays)
        stats.safetiesCommitted = gameStatsCalculator.calculateSafeties(allOffensivePlays)
        stats.safetiesForced = gameStatsCalculator.calculateSafeties(allDefensivePlays)
        stats.averageResponseSpeed = gameStatsCalculator.calculateAverageResponseSpeed(allPlays, teamSide)
        stats.gameStatus = game.gameStatus
        stats.averageDiff = gameStatsCalculator.calculateAverageDiff(allPlays)
        stats.lastModifiedTs =
            ZonedDateTime.now(
                ZoneId.of("America/New_York"),
            ).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        return stats
    }

    private fun updateScoreStats(
        allPlays: List<Play>,
        stats: GameStats,
        teamSide: TeamSide,
    ) {
        stats.q1Score = 0
        stats.q2Score = 0
        stats.q3Score = 0
        stats.q4Score = 0
        stats.otScore = 0

        for (play in allPlays) {
            if (play.quarter == 1) {
                stats.q1Score = gameStatsCalculator.calculateQuarterScore(play, stats.q1Score, teamSide)
            }
            if (play.quarter == 2) {
                stats.q2Score = gameStatsCalculator.calculateQuarterScore(play, stats.q2Score, teamSide)
            }
            if (play.quarter == 3) {
                stats.q3Score = gameStatsCalculator.calculateQuarterScore(play, stats.q3Score, teamSide)
            }
            if (play.quarter == 4) {
                stats.q4Score = gameStatsCalculator.calculateQuarterScore(play, stats.q4Score, teamSide)
            }
            if (play.quarter >= 5) {
                stats.otScore = gameStatsCalculator.calculateQuarterScore(play, stats.otScore, teamSide)
            }
        }
    }

    fun getAllGameStatsForTeamAndSeason(
        team: String,
        season: Int,
    ): List<GameStats> {
        return gameStatsRepository.findByTeamAndSeason(team, season)
    }

    fun getGameStatsBySeasonAndWeek(
        season: Int,
        week: Int?,
    ): List<GameStats> {
        return if (week != null) {
            gameStatsRepository.getGameStatsBySeasonAndWeek(season, week)
        } else {
            gameStatsRepository.findBySeasonOrderByGameIdAsc(season)
        }
    }
}
