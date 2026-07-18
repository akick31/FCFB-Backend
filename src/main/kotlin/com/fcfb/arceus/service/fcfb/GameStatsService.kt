package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.fcfb.gamestats.GameStatsCalculator
import com.fcfb.arceus.util.GameNotFoundException
import com.fcfb.arceus.util.GameStatsNotFoundException
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class GameStatsService(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val teamRepository: TeamRepository,
) {
    fun createGameStats(game: Game): List<GameStats> {
        // Get current ELO for both teams
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

    fun generateGameStatsForGamesMoreRecentThanGameId(gameId: Int) {
        try {
            val allGames =
                gameRepository.getAllGamesMoreRecentThanGameId(gameId).ifEmpty {
                    throw GameNotFoundException("Could not find any games more recent than game ID $gameId")
                }

            for (game in allGames) {
                Logger.info("Generating game stats for game ${game.gameId}")
                generateGameStats(game.gameId)
            }
        } catch (e: Exception) {
            throw Exception("Could not generate game stats for games more recent than game ID $gameId", e)
        }
    }

    fun generateAllGameStats() {
        try {
            val allGames =
                gameRepository.getAllGames().ifEmpty {
                    throw GameNotFoundException("Could not find any games")
                }

            for (game in allGames) {
                Logger.info("Generating game stats for game ${game.gameId}")
                generateGameStats(game.gameId)
            }
        } catch (e: Exception) {
            throw Exception("Could not generate game stats", e)
        }
    }

    fun generateGameStats(gameId: Int) {
        // Get previous ranking
        // Delete previous stats from game
        deleteByGameId(gameId)

        // Create new game stats for game
        val game =
            gameRepository.getGameById(gameId)
                ?: throw Exception("Could not find game with ID $gameId")
        createGameStats(game)

        // Get game and all plays
        val allPlays = playRepository.getAllPlaysByGameId(gameId)

        // Update the game stats
        updateGameStats(game, allPlays)
    }

    fun updateGameStats(
        game: Game,
        allPlays: List<Play>,
    ): List<GameStats> {
        var homeStats = getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
        updateScoreStats(allPlays, homeStats, TeamSide.HOME)
        homeStats = updateStats(allPlays, TeamSide.HOME, game, homeStats)

        var awayStats = getGameStatsByIdAndTeam(game.gameId, game.awayTeam)
        updateScoreStats(allPlays, awayStats, TeamSide.AWAY)
        awayStats = updateStats(allPlays, TeamSide.AWAY, game, awayStats)
        return listOf(homeStats, awayStats)
    }

    fun saveGameStats(gameStats: GameStats) = gameStatsRepository.save(gameStats)

    fun getGameStatsByIdAndTeam(
        gameId: Int,
        team: String,
    ) = gameStatsRepository.getGameStatsByIdAndTeam(gameId, team)
        ?: throw GameStatsNotFoundException("Could not find game stats for game $gameId and team $team")

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

        stats.score = game.homeScore
        stats.passAttempts = GameStatsCalculator.calculatePassAttempts(allOffensivePlays)
        stats.passCompletions = GameStatsCalculator.calculatePassCompletions(allOffensivePlays)
        stats.passCompletionPercentage = GameStatsCalculator.calculatePercentage(stats.passCompletions, stats.passAttempts)
        stats.passYards = GameStatsCalculator.calculatePassYards(allOffensivePlays)
        stats.longestPass = GameStatsCalculator.calculateLongestPass(allOffensivePlays)
        stats.sacksAllowed = GameStatsCalculator.calculateSacksAllowed(allOffensivePlays)
        stats.sacksForced = GameStatsCalculator.calculateSacksAllowed(allDefensivePlays)
        stats.rushAttempts = GameStatsCalculator.calculateRushAttempts(allOffensivePlays)
        stats.rushSuccesses = GameStatsCalculator.calculateRushSuccesses(allOffensivePlays)
        stats.rushSuccessPercentage = GameStatsCalculator.calculatePercentage(stats.rushSuccesses, stats.rushAttempts)
        stats.passSuccesses = GameStatsCalculator.calculatePassSuccesses(allOffensivePlays)
        stats.passSuccessPercentage = GameStatsCalculator.calculatePercentage(stats.passSuccesses, stats.passAttempts)
        stats.rushYards = GameStatsCalculator.calculateRushYards(allOffensivePlays)
        stats.longestRun = GameStatsCalculator.calculateLongestRun(allOffensivePlays)
        stats.totalYards = GameStatsCalculator.calculateTotalYards(stats.passYards, stats.rushYards)
        stats.interceptionsLost = GameStatsCalculator.calculateInterceptionsLost(allOffensivePlays)
        stats.interceptionsForced = GameStatsCalculator.calculateInterceptionsLost(allDefensivePlays)
        stats.fumblesLost = GameStatsCalculator.calculateFumblesLost(allOffensivePlays)
        stats.fumblesForced = GameStatsCalculator.calculateFumblesLost(allDefensivePlays)
        stats.turnoversLost = GameStatsCalculator.calculateTurnoversLost(stats.interceptionsLost, stats.fumblesLost)
        stats.turnoversForced = GameStatsCalculator.calculateTurnoversLost(stats.interceptionsForced, stats.fumblesForced)
        stats.turnoverTouchdownsLost = GameStatsCalculator.calculateTurnoverTouchdownsLost(allOffensivePlays)
        stats.turnoverTouchdownsForced = GameStatsCalculator.calculateTurnoverTouchdownsLost(allOffensivePlays)
        stats.fieldGoalMade = GameStatsCalculator.calculateFieldGoalMade(allOffensivePlays)
        stats.fieldGoalAttempts = GameStatsCalculator.calculateFieldGoalAttempts(allOffensivePlays)
        stats.fieldGoalPercentage = GameStatsCalculator.calculatePercentage(stats.fieldGoalMade, stats.fieldGoalAttempts)
        stats.longestFieldGoal = GameStatsCalculator.calculateLongestFieldGoal(allOffensivePlays)
        stats.blockedOpponentFieldGoals = GameStatsCalculator.calculateBlockedOpponentFieldGoals(allDefensivePlays)
        stats.fieldGoalTouchdown = GameStatsCalculator.calculateFieldGoalTouchdown(allOffensivePlays)
        stats.puntsAttempted = GameStatsCalculator.calculatePuntsAttempted(allOffensivePlays)
        stats.longestPunt = GameStatsCalculator.calculateLongestPunt(allOffensivePlays)
        stats.averagePuntLength = GameStatsCalculator.calculateAveragePuntLength(allOffensivePlays)
        stats.blockedOpponentPunt = GameStatsCalculator.calculateBlockedOpponentPunt(allOffensivePlays)
        stats.puntReturnTd = GameStatsCalculator.calculatePuntReturnTd(allOffensivePlays)
        stats.puntReturnTdPercentage =
            GameStatsCalculator.calculatePercentage(
                stats.puntReturnTd,
                GameStatsCalculator.calculatePuntsAttempted(allDefensivePlays),
            )
        stats.numberOfKickoffs = GameStatsCalculator.calculateNumberOfKickoffs(allDefensivePlays)
        stats.onsideAttempts = GameStatsCalculator.calculateOnsideAttempts(allOffensivePlays)
        stats.onsideSuccess = GameStatsCalculator.calculateOnsideSuccess(allOffensivePlays)
        stats.onsideSuccessPercentage = GameStatsCalculator.calculatePercentage(stats.onsideSuccess, stats.onsideAttempts)
        stats.normalKickoffAttempts = GameStatsCalculator.calculateNormalKickoffAttempts(allOffensivePlays)
        stats.touchbacks = GameStatsCalculator.calculateTouchbacks(allOffensivePlays)
        stats.touchbackPercentage = GameStatsCalculator.calculatePercentage(stats.touchbacks, stats.normalKickoffAttempts)
        stats.kickReturnTd = GameStatsCalculator.calculateKickReturnTd(allDefensivePlays)
        stats.kickReturnTdPercentage = GameStatsCalculator.calculatePercentage(stats.kickReturnTd, stats.numberOfKickoffs)
        stats.numberOfDrives = GameStatsCalculator.calculateNumberOfDrives(allPlays, teamSide)
        stats.timeOfPossession = GameStatsCalculator.calculateTimeOfPossession(allOffensivePlays)
        stats.touchdowns = GameStatsCalculator.calculateTouchdowns(allPlays, teamSide)
        stats.averageOffensiveDiff = GameStatsCalculator.calculateAverageNormalPlayDiff(allOffensivePlays)
        stats.averageDefensiveDiff = GameStatsCalculator.calculateAverageNormalPlayDiff(allDefensivePlays)
        stats.averageOffensiveSpecialTeamsDiff = GameStatsCalculator.calculateAverageSpecialTeamsDiff(allOffensivePlays)
        stats.averageDefensiveSpecialTeamsDiff = GameStatsCalculator.calculateAverageSpecialTeamsDiff(allDefensivePlays)
        stats.averageYardsPerPlay = GameStatsCalculator.calculateAverageYardsPerPlay(allOffensivePlays)
        stats.firstDowns = GameStatsCalculator.calculateFirstDowns(allOffensivePlays)
        stats.thirdDownConversionSuccess = GameStatsCalculator.calculateThirdDownConversionSuccess(allOffensivePlays)
        stats.thirdDownConversionAttempts = GameStatsCalculator.calculateThirdDownConversionAttempts(allOffensivePlays)
        stats.thirdDownConversionPercentage =
            GameStatsCalculator.calculatePercentage(stats.thirdDownConversionSuccess, stats.thirdDownConversionAttempts)
        stats.fourthDownConversionSuccess = GameStatsCalculator.calculateFourthDownConversionSuccess(allOffensivePlays)
        stats.fourthDownConversionAttempts = GameStatsCalculator.calculateFourthDownConversionAttempts(allOffensivePlays)
        stats.fourthDownConversionPercentage =
            GameStatsCalculator.calculatePercentage(stats.fourthDownConversionSuccess, stats.fourthDownConversionAttempts)
        if (teamSide == TeamSide.HOME) {
            stats.largestLead = GameStatsCalculator.calculateLargestLeadForHome(allPlays)
            stats.largestDeficit = GameStatsCalculator.calculateLargestDeficitForHome(allPlays)
        } else {
            stats.largestLead = GameStatsCalculator.calculateLargestLeadForAway(allPlays)
            stats.largestDeficit = GameStatsCalculator.calculateLargestDeficitForAway(allPlays)
        }
        stats.passTouchdowns = GameStatsCalculator.calculatePassTouchdowns(allOffensivePlays)
        stats.rushTouchdowns = GameStatsCalculator.calculateRushTouchdowns(allOffensivePlays)
        stats.redZoneAttempts = GameStatsCalculator.calculateRedZoneAttempts(allPlays, teamSide)
        stats.redZoneSuccesses = GameStatsCalculator.calculateRedZoneSuccesses(allOffensivePlays)
        stats.redZoneSuccessPercentage = GameStatsCalculator.calculatePercentage(stats.redZoneSuccesses, stats.redZoneAttempts)
        stats.redZonePercentage = GameStatsCalculator.calculatePercentage(stats.redZoneAttempts, stats.numberOfDrives)
        stats.turnoverDifferential = GameStatsCalculator.calculateTurnoverDifferential(stats.turnoversLost, stats.turnoversForced)
        stats.pickSixesThrown = GameStatsCalculator.calculatePickSixes(allOffensivePlays)
        stats.pickSixesForced = GameStatsCalculator.calculatePickSixes(allDefensivePlays)
        stats.fumbleReturnTdsCommitted = GameStatsCalculator.calculateFumbleReturnTds(allOffensivePlays)
        stats.fumbleReturnTdsForced = GameStatsCalculator.calculateFumbleReturnTds(allDefensivePlays)
        stats.safetiesCommitted = GameStatsCalculator.calculateSafeties(allOffensivePlays)
        stats.safetiesForced = GameStatsCalculator.calculateSafeties(allDefensivePlays)
        stats.averageResponseSpeed = GameStatsCalculator.calculateAverageResponseSpeed(allPlays, teamSide)
        stats.gameStatus = game.gameStatus
        stats.averageDiff = GameStatsCalculator.calculateAverageDiff(allPlays)
        stats.lastModifiedTs =
            ZonedDateTime.now(
                ZoneId.of("America/New_York"),
            ).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        saveGameStats(stats)
        return stats
    }

    private fun updateScoreStats(
        allPlays: List<Play>,
        stats: GameStats,
        teamSide: TeamSide,
    ) {
        // Reset quarter scores before recalculating from all plays
        stats.q1Score = 0
        stats.q2Score = 0
        stats.q3Score = 0
        stats.q4Score = 0
        stats.otScore = 0

        for (play in allPlays) {
            if (play.quarter == 1) {
                stats.q1Score = GameStatsCalculator.calculateQuarterScore(play, stats.q1Score, teamSide)
            }
            if (play.quarter == 2) {
                stats.q2Score = GameStatsCalculator.calculateQuarterScore(play, stats.q2Score, teamSide)
            }
            if (play.quarter == 3) {
                stats.q3Score = GameStatsCalculator.calculateQuarterScore(play, stats.q3Score, teamSide)
            }
            if (play.quarter == 4) {
                stats.q4Score = GameStatsCalculator.calculateQuarterScore(play, stats.q4Score, teamSide)
            }
            if (play.quarter == 5) {
                stats.otScore = GameStatsCalculator.calculateQuarterScore(play, stats.otScore, teamSide)
            }
        }
    }

    fun getAllGameStatsForTeamAndSeason(
        team: String,
        season: Int,
    ): List<GameStats> {
        return gameStatsRepository.findByTeamAndSeason(team, season)
    }

    /**
     * Get ELO history for a team
     * @param team Team name (or "all" for all teams)
     * @param season Season number (optional, null for all-time)
     * @return List of ELO history entries
     */
    fun getEloHistory(
        team: String,
        season: Int?,
    ): List<com.fcfb.arceus.dto.response.EloHistoryEntry> {
        val gameStatsList =
            if (team.lowercase() == "all") {
                // Get all teams' ELO history - use optimized query
                // For "all teams", limit to most recent data to avoid timeout
                if (season != null) {
                    gameStatsRepository.findBySeasonOrderByGameIdAsc(season)
                } else {
                    // For all-time, get only the most recent seasons (last 10 seasons) to avoid timeout
                    // This is a reasonable limit for visualization while still showing meaningful history
                    // Get the latest season directly from database
                    val latestSeason = gameStatsRepository.findMaxSeason() ?: 0
                    if (latestSeason == 0) {
                        emptyList()
                    } else {
                        val minSeason = (latestSeason - 9).coerceAtLeast(1)
                        // Use optimized query that filters at database level
                        gameStatsRepository.findBySeasonGreaterThanEqualOrderBySeasonDescGameIdAsc(minSeason)
                    }
                }
            } else {
                // Get single team's ELO history
                if (season != null) {
                    gameStatsRepository.findByTeamAndSeason(team, season)
                } else {
                    gameStatsRepository.findByTeam(team)
                }
            }

        if (gameStatsList.isEmpty()) {
            return emptyList()
        }

        // Batch fetch all games to avoid N+1 query problem
        // Limit to 10,000 games max to avoid memory issues
        val gameIds = gameStatsList.mapNotNull { it.gameId }.distinct().take(10000)
        val gamesMap =
            if (gameIds.isNotEmpty()) {
                gameRepository.findAllById(gameIds).associateBy { it.gameId }
            } else {
                emptyMap()
            }

        // Sort by team, then season, then week, then game ID
        val sortedStats =
            gameStatsList.sortedWith(
                compareBy<GameStats> { it.team ?: "" }
                    .thenBy { it.season ?: 0 }
                    .thenBy { it.week ?: 0 }
                    .thenBy { it.gameId },
            )

        return sortedStats.map { stats ->
            val teamName = stats.team ?: ""
            val game = stats.gameId?.let { gamesMap[it] }
            val opponent =
                if (game != null) {
                    if (game.homeTeam == teamName) game.awayTeam else game.homeTeam
                } else {
                    null
                }

            val result =
                if (game != null && game.gameStatus?.name == "FINAL") {
                    val teamScore = if (game.homeTeam == teamName) game.homeScore else game.awayScore
                    val oppScore = if (game.homeTeam == teamName) game.awayScore else game.homeScore
                    when {
                        teamScore != null && oppScore != null -> {
                            if (teamScore > oppScore) {
                                "W"
                            } else if (teamScore < oppScore) {
                                "L"
                            } else {
                                null
                            }
                        }
                        else -> null
                    }
                } else {
                    null
                }

            com.fcfb.arceus.dto.response.EloHistoryEntry(
                team = teamName,
                season = stats.season ?: 0,
                week = stats.week,
                elo = stats.teamElo,
                gameId = stats.gameId,
                opponent = opponent,
                result = result,
            )
        }
    }

    /**
     * Get game stats by season and week with opponent stats included
     * @param season Season number
     * @param week Week number (optional, null for entire season)
     * @return List of game stats (frontend will pair with opponents by game_id)
     */
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
