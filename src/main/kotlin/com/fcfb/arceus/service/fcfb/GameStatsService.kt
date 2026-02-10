package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.repositories.TeamRepository
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
    /**
     * Create a game stats entry
     * @param game
     */
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

    /**
     * Generate game stats for all games more recent than the given game ID
     */
    fun generateGameStatsForGamesMoreRecentThanGameId(gameId: Int) {
        try {
            // Get all games more recent than the given game ID
            val allGames =
                gameRepository.getAllGamesMoreRecentThanGameId(gameId).ifEmpty {
                    throw GameNotFoundException("Could not find any games more recent than game ID $gameId")
                }

            // Iterate through the games and generate the game stats
            for (game in allGames) {
                Logger.info("Generating game stats for game ${game.gameId}")
                generateGameStats(game.gameId)
            }
        } catch (e: Exception) {
            throw Exception("Could not generate game stats for games more recent than game ID $gameId")
        }
    }

    /**
     * Generate game stats for all games
     */
    fun generateAllGameStats() {
        try {
            // Get all games
            val allGames =
                gameRepository.getAllGames().ifEmpty {
                    throw GameNotFoundException("Could not find any games")
                }

            // Iterate through the games and generate the game stats
            for (game in allGames) {
                Logger.info("Generating game stats for game ${game.gameId}")
                generateGameStats(game.gameId)
            }
        } catch (e: Exception) {
            throw Exception("Could not generate game stats")
        }
    }

    /**
     * Generate game stats for a game
     */
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

    /**
     * Update the game stats for the current game
     */
    fun updateGameStats(
        game: Game,
        allPlays: List<Play>,
    ): List<GameStats> {
        var homeStats = getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
        updateScoreStats(allPlays, homeStats)
        homeStats = updateStats(allPlays, TeamSide.HOME, game, homeStats)

        var awayStats = getGameStatsByIdAndTeam(game.gameId, game.awayTeam)
        updateScoreStats(allPlays, awayStats)
        awayStats = updateStats(allPlays, TeamSide.AWAY, game, awayStats)
        return listOf(homeStats, awayStats)
    }

    /**
     * Save game stats entry
     * @param gameStats
     */
    fun saveGameStats(gameStats: GameStats) = gameStatsRepository.save(gameStats)

    /**
     * Get game stats entry by game ID
     * @param gameId
     */
    fun getGameStatsByIdAndTeam(
        gameId: Int,
        team: String,
    ) = gameStatsRepository.getGameStatsByIdAndTeam(gameId, team)
        ?: throw GameStatsNotFoundException("Could not find game stats for game $gameId and team $team")

    /**
     * Get game stats by game id
     */
    fun getGameStatsById(gameId: Int) = gameStatsRepository.findByGameId(gameId)

    /**
     * Delete game stats entry by game ID
     */
    fun deleteByGameId(gameId: Int) = gameStatsRepository.deleteByGameId(gameId)

    /**
     * Calculate the stats for each team based on team side
     */
    private fun updateStats(
        allPlays: List<Play>,
        teamSide: TeamSide,
        game: Game,
        stats: GameStats,
    ): GameStats {
        val allOffensivePlays = allPlays.filter { play -> play.possession == teamSide }
        val allDefensivePlays = allPlays.filter { play -> play.possession != teamSide }

        stats.score = game.homeScore
        stats.passAttempts = calculatePassAttempts(allOffensivePlays)
        stats.passCompletions = calculatePassCompletions(allOffensivePlays)
        stats.passCompletionPercentage = calculatePercentage(stats.passCompletions, stats.passAttempts)
        stats.passYards = calculatePassYards(allOffensivePlays)
        stats.longestPass = calculateLongestPass(allOffensivePlays)
        stats.sacksAllowed = calculateSacksAllowed(allOffensivePlays)
        stats.sacksForced = calculateSacksAllowed(allDefensivePlays)
        stats.rushAttempts = calculateRushAttempts(allOffensivePlays)
        stats.rushSuccesses = calculateRushSuccesses(allOffensivePlays)
        stats.rushSuccessPercentage = calculatePercentage(stats.rushSuccesses, stats.rushAttempts)
        stats.passSuccesses = calculatePassSuccesses(allOffensivePlays)
        stats.passSuccessPercentage = calculatePercentage(stats.passSuccesses, stats.passAttempts)
        stats.rushYards = calculateRushYards(allOffensivePlays)
        stats.longestRun = calculateLongestRun(allOffensivePlays)
        stats.totalYards = calculateTotalYards(stats.passYards, stats.rushYards)
        stats.interceptionsLost = calculateInterceptionsLost(allOffensivePlays)
        stats.interceptionsForced = calculateInterceptionsLost(allDefensivePlays)
        stats.fumblesLost = calculateFumblesLost(allOffensivePlays)
        stats.fumblesForced = calculateFumblesLost(allDefensivePlays)
        stats.turnoversLost = calculateTurnoversLost(stats.interceptionsLost, stats.fumblesLost)
        stats.turnoversForced = calculateTurnoversLost(stats.interceptionsForced, stats.fumblesForced)
        stats.turnoverTouchdownsLost = calculateTurnoverTouchdownsLost(allOffensivePlays)
        stats.turnoverTouchdownsForced = calculateTurnoverTouchdownsLost(allOffensivePlays)
        stats.fieldGoalMade = calculateFieldGoalMade(allOffensivePlays)
        stats.fieldGoalAttempts = calculateFieldGoalAttempts(allOffensivePlays)
        stats.fieldGoalPercentage = calculatePercentage(stats.fieldGoalMade, stats.fieldGoalAttempts)
        stats.longestFieldGoal = calculateLongestFieldGoal(allOffensivePlays)
        stats.blockedOpponentFieldGoals = calculateBlockedOpponentFieldGoals(allDefensivePlays)
        stats.fieldGoalTouchdown = calculateFieldGoalTouchdown(allOffensivePlays)
        stats.puntsAttempted = calculatePuntsAttempted(allOffensivePlays)
        stats.longestPunt = calculateLongestPunt(allOffensivePlays)
        stats.averagePuntLength = calculateAveragePuntLength(allOffensivePlays)
        stats.blockedOpponentPunt = calculateBlockedOpponentPunt(allOffensivePlays)
        stats.puntReturnTd = calculatePuntReturnTd(allOffensivePlays)
        stats.puntReturnTdPercentage = calculatePercentage(stats.puntReturnTd, calculatePuntsAttempted(allDefensivePlays))
        stats.numberOfKickoffs = calculateNumberOfKickoffs(allDefensivePlays)
        stats.onsideAttempts = calculateOnsideAttempts(allOffensivePlays)
        stats.onsideSuccess = calculateOnsideSuccess(allOffensivePlays)
        stats.onsideSuccessPercentage = calculatePercentage(stats.onsideSuccess, stats.onsideAttempts)
        stats.normalKickoffAttempts = calculateNormalKickoffAttempts(allOffensivePlays)
        stats.touchbacks = calculateTouchbacks(allOffensivePlays)
        stats.touchbackPercentage = calculatePercentage(stats.touchbacks, stats.normalKickoffAttempts)
        stats.kickReturnTd = calculateKickReturnTd(allDefensivePlays)
        stats.kickReturnTdPercentage = calculatePercentage(stats.kickReturnTd, stats.numberOfKickoffs)
        stats.numberOfDrives = calculateNumberOfDrives(allPlays, teamSide)
        stats.timeOfPossession = calculateTimeOfPossession(allOffensivePlays)
        stats.touchdowns = calculateTouchdowns(allPlays, teamSide)
        stats.averageOffensiveDiff = calculateAverageNormalPlayDiff(allOffensivePlays)
        stats.averageDefensiveDiff = calculateAverageNormalPlayDiff(allDefensivePlays)
        stats.averageOffensiveSpecialTeamsDiff = calculateAverageSpecialTeamsDiff(allOffensivePlays)
        stats.averageDefensiveSpecialTeamsDiff = calculateAverageSpecialTeamsDiff(allDefensivePlays)
        stats.averageYardsPerPlay = calculateAverageYardsPerPlay(allOffensivePlays)
        stats.firstDowns = calculateFirstDowns(allOffensivePlays)
        stats.thirdDownConversionSuccess = calculateThirdDownConversionSuccess(allOffensivePlays)
        stats.thirdDownConversionAttempts = calculateThirdDownConversionAttempts(allOffensivePlays)
        stats.thirdDownConversionPercentage =
            calculatePercentage(stats.thirdDownConversionSuccess, stats.thirdDownConversionAttempts)
        stats.fourthDownConversionSuccess = calculateFourthDownConversionSuccess(allOffensivePlays)
        stats.fourthDownConversionAttempts = calculateFourthDownConversionAttempts(allOffensivePlays)
        stats.fourthDownConversionPercentage =
            calculatePercentage(stats.fourthDownConversionSuccess, stats.fourthDownConversionAttempts)
        if (teamSide == TeamSide.HOME) {
            stats.largestLead = calculateLargestLeadForHome(allPlays)
            stats.largestDeficit = calculateLargestDeficitForHome(allPlays)
        } else {
            stats.largestLead = calculateLargestLeadForAway(allPlays)
            stats.largestDeficit = calculateLargestDeficitForAway(allPlays)
        }
        stats.passTouchdowns = calculatePassTouchdowns(allOffensivePlays)
        stats.rushTouchdowns = calculateRushTouchdowns(allOffensivePlays)
        stats.redZoneAttempts = calculateRedZoneAttempts(allPlays, teamSide)
        stats.redZoneSuccesses = calculateRedZoneSuccesses(allOffensivePlays)
        stats.redZoneSuccessPercentage = calculatePercentage(stats.redZoneSuccesses, stats.redZoneAttempts)
        stats.redZonePercentage = calculatePercentage(stats.redZoneAttempts, stats.numberOfDrives)
        stats.turnoverDifferential = calculateTurnoverDifferential(stats.turnoversLost, stats.turnoversForced)
        stats.pickSixesThrown = calculatePickSixes(allOffensivePlays)
        stats.pickSixesForced = calculatePickSixes(allDefensivePlays)
        stats.fumbleReturnTdsCommitted = calculateFumbleReturnTds(allOffensivePlays)
        stats.fumbleReturnTdsForced = calculateFumbleReturnTds(allDefensivePlays)
        stats.safetiesCommitted = calculateSafeties(allOffensivePlays)
        stats.safetiesForced = calculateSafeties(allDefensivePlays)
        stats.averageResponseSpeed = calculateAverageResponseSpeed(allPlays, teamSide)
        stats.gameStatus = game.gameStatus
        stats.averageDiff = calculateAverageDiff(allPlays)
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
    ) {
        for (play in allPlays) {
            if (play.quarter == 1) {
                stats.q1Score = calculateQuarterScore(play, stats.q1Score, play.possession)
            }
            if (play.quarter == 2) {
                stats.q2Score = calculateQuarterScore(play, stats.q2Score, play.possession)
            }
            if (play.quarter == 3) {
                stats.q3Score = calculateQuarterScore(play, stats.q3Score, play.possession)
            }
            if (play.quarter == 4) {
                stats.q4Score = calculateQuarterScore(play, stats.q4Score, play.possession)
            }
            if (play.quarter == 5) {
                stats.otScore = calculateQuarterScore(play, stats.otScore, play.possession)
            }
        }
    }

    private fun calculatePassAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            when {
                play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS -> false
                play.playCall == PlayCall.SPIKE -> true
                play.playCall == PlayCall.PASS -> true
                else -> false
            }
        }
    }

    private fun calculatePassCompletions(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            when {
                play.playCall == PlayCall.PASS && (
                    play.result != Scenario.INCOMPLETE &&
                        play.result != Scenario.LOSS_OF_10_YARDS &&
                        play.result != Scenario.LOSS_OF_7_YARDS &&
                        play.result != Scenario.LOSS_OF_5_YARDS &&
                        play.result != Scenario.LOSS_OF_3_YARDS &&
                        play.result != Scenario.LOSS_OF_2_YARDS &&
                        play.result != Scenario.LOSS_OF_1_YARD &&
                        play.result != Scenario.TURNOVER_PLUS_20_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_15_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_10_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_5_YARDS &&
                        play.result != Scenario.TURNOVER &&
                        play.result != Scenario.TURNOVER_MINUS_5_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_10_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_15_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_20_YARDS &&
                        play.result != Scenario.TURNOVER_TOUCHDOWN &&
                        play.result != Scenario.SAFETY
                ) -> true
                else -> false
            }
        }
    }

    private fun calculatePassYards(allPlays: List<Play>): Int {
        return allPlays.sumOf { play ->
            // Don't count sacks towards passing yards
            when {
                play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS -> 0
                play.playCall == PlayCall.PASS -> play.yards
                else -> 0
            }
        }
    }

    private fun calculateLongestPass(allPlays: List<Play>): Int {
        return allPlays
            .filter { play ->
                play.playCall == PlayCall.PASS && play.result != Scenario.INCOMPLETE
            }
            .maxOfOrNull { it.yards } ?: 0
    }

    private fun calculateSacksAllowed(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS
        }
    }

    private fun calculateRushAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.RUN
        }
    }

    private fun calculateRushSuccesses(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            if (play.playCall != PlayCall.RUN) return@count false

            val yardsToGo = play.yardsToGo
            val yardsGained = play.yards
            val down = play.down

            val isSuccess =
                when (down) {
                    1 -> yardsGained >= (yardsToGo * 0.5)
                    2 -> yardsGained >= (yardsToGo * 0.7)
                    3, 4 -> yardsGained >= yardsToGo
                    else -> false
                }

            isSuccess || play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    private fun calculateRushYards(allPlays: List<Play>): Int {
        return allPlays.sumOf { play ->
            when {
                // Count sacks towards rushing yards
                play.playCall == PlayCall.PASS && (
                    play.result == Scenario.LOSS_OF_10_YARDS ||
                        play.result == Scenario.LOSS_OF_7_YARDS ||
                        play.result == Scenario.LOSS_OF_5_YARDS ||
                        play.result == Scenario.LOSS_OF_3_YARDS ||
                        play.result == Scenario.LOSS_OF_2_YARDS ||
                        play.result == Scenario.LOSS_OF_1_YARD
                ) -> play.yards

                play.playCall == PlayCall.RUN && (
                    play.result != Scenario.TURNOVER_PLUS_20_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_15_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_10_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_5_YARDS &&
                        play.result != Scenario.TURNOVER &&
                        play.result != Scenario.TURNOVER_MINUS_5_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_10_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_15_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_20_YARDS &&
                        play.result != Scenario.TURNOVER_TOUCHDOWN &&
                        play.result != Scenario.SAFETY
                ) -> play.yards

                else -> 0
            }
        }
    }

    private fun calculateLongestRun(allPlays: List<Play>): Int {
        return allPlays
            .filter { play ->
                play.playCall == PlayCall.RUN && play.result != Scenario.INCOMPLETE
            }
            .maxOfOrNull { it.yards } ?: 0
    }

    private fun calculatePassSuccesses(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            if (play.playCall != PlayCall.PASS) return@count false

            val yardsToGo = play.yardsToGo
            val yardsGained = play.yards
            val down = play.down

            val isSuccess =
                when (down) {
                    1 -> yardsGained >= (yardsToGo * 0.5)
                    2 -> yardsGained >= (yardsToGo * 0.7)
                    3, 4 -> yardsGained >= yardsToGo
                    else -> false
                }

            isSuccess || play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    private fun calculateTotalYards(
        passingYards: Int,
        rushingYards: Int,
    ): Int {
        return passingYards + rushingYards
    }

    private fun calculateInterceptionsLost(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PASS && (
                play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
            )
        }
    }

    private fun calculateFumblesLost(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.RUN && (
                play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
            )
        }
    }

    private fun calculateTurnoversLost(
        interceptionsLost: Int,
        fumblesLost: Int,
    ): Int {
        return interceptionsLost + fumblesLost
    }

    private fun calculateTurnoverTouchdownsLost(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
        }
    }

    private fun calculateFieldGoalMade(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD
        }
    }

    private fun calculateFieldGoalAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.FIELD_GOAL
        }
    }

    private fun calculateLongestFieldGoal(allPlays: List<Play>): Int {
        return allPlays
            .filter { play ->
                play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD
            }
            .maxOfOrNull { play ->
                (100 - play.ballLocation) + 17
            } ?: 0
    }

    private fun calculateBlockedOpponentFieldGoals(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.FIELD_GOAL && play.actualResult == ActualResult.BLOCKED
        }
    }

    private fun calculateFieldGoalTouchdown(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.FIELD_GOAL && play.actualResult == ActualResult.KICK_SIX
        }
    }

    private fun calculatePuntsAttempted(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PUNT
        }
    }

    private fun calculateLongestPunt(allPlays: List<Play>): Int {
        val puntDistances =
            listOf(
                Scenario.FIVE_YARD_PUNT, Scenario.TEN_YARD_PUNT, Scenario.FIFTEEN_YARD_PUNT, Scenario.TWENTY_YARD_PUNT,
                Scenario.TWENTY_FIVE_YARD_PUNT, Scenario.THIRTY_YARD_PUNT, Scenario.THIRTY_FIVE_YARD_PUNT,
                Scenario.FORTY_YARD_PUNT, Scenario.FORTY_FIVE_YARD_PUNT, Scenario.FIFTY_YARD_PUNT,
                Scenario.FIFTY_FIVE_YARD_PUNT, Scenario.SIXTY_YARD_PUNT, Scenario.SIXTY_FIVE_YARD_PUNT,
                Scenario.SEVENTY_YARD_PUNT,
            )

        return allPlays
            .filter { play -> play.result in puntDistances }
            .maxOfOrNull { play ->
                play.result?.description?.substringBefore(" YARD PUNT")?.toInt() ?: 0
            } ?: 0
    }

    private fun calculateAveragePuntLength(allPlays: List<Play>): Double {
        val average =
            allPlays
                .filter { play ->
                    play.playCall == PlayCall.PUNT &&
                        play.result?.description?.contains(" YARD PUNT") ?: false
                }
                .map { play ->
                    play.result?.description?.substringBefore(" YARD PUNT")?.toInt() ?: 0
                }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateBlockedOpponentPunt(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PUNT && play.actualResult == ActualResult.BLOCKED
        }
    }

    private fun calculatePuntReturnTd(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PUNT && play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN
        }
    }

    private fun calculateNumberOfKickoffs(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_NORMAL ||
                play.playCall == PlayCall.KICKOFF_ONSIDE ||
                play.playCall == PlayCall.KICKOFF_SQUIB
        }
    }

    private fun calculateOnsideAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_ONSIDE
        }
    }

    private fun calculateOnsideSuccess(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_ONSIDE && play.result == Scenario.RECOVERED
        }
    }

    private fun calculateNormalKickoffAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_NORMAL
        }
    }

    private fun calculateTouchbacks(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_NORMAL && play.result == Scenario.TOUCHBACK
        }
    }

    private fun calculateKickReturnTd(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_NORMAL && play.actualResult == ActualResult.RETURN_TOUCHDOWN
        }
    }

    private fun calculateNumberOfDrives(
        allPlays: List<Play>,
        teamSide: TeamSide,
    ): Int {
        var driveCount = 0
        var isDriveInProgress = false

        allPlays.sortedBy { it.playId }.forEach { play ->
            when {
                // If the current play is a kickoff, end the current drive
                (
                    play.playCall == PlayCall.KICKOFF_NORMAL ||
                        play.playCall == PlayCall.KICKOFF_ONSIDE ||
                        play.playCall == PlayCall.KICKOFF_SQUIB
                ) && play.possession == teamSide -> {
                    isDriveInProgress = false
                }

                // Player starts or continues a drive (possession belongs to the player)
                play.possession == teamSide && !isDriveInProgress -> {
                    // Start a new drive
                    driveCount++
                    isDriveInProgress = true
                }

                // If possession changes to another player or a turnover happens
                play.possession != teamSide ||
                    play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN -> {
                    // End the current drive
                    isDriveInProgress = false
                }
            }
        }

        return driveCount
    }

    private fun calculateTimeOfPossession(allPlays: List<Play>): Int {
        return allPlays
            .filter { play ->
                play.playCall != PlayCall.KICKOFF_NORMAL &&
                    play.playCall != PlayCall.KICKOFF_ONSIDE &&
                    play.playCall != PlayCall.KICKOFF_SQUIB
            }
            .sumOf { play ->
                if (play.clock - play.playTime + play.runoffTime < 0) {
                    play.clock
                } else {
                    play.playTime + play.runoffTime
                }
            }
    }

    private fun calculateTouchdowns(
        allPlays: List<Play>,
        teamSide: TeamSide,
    ): Int {
        val offensiveTouchdowns =
            allPlays.count { play ->
                play.possession == teamSide && (
                    play.actualResult == ActualResult.TOUCHDOWN ||
                        play.actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
                        play.actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN
                )
            }

        val defensiveTouchdowns =
            allPlays.count { play ->
                play.possession != teamSide && (
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
                        play.actualResult == ActualResult.RETURN_TOUCHDOWN ||
                        play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
                        play.actualResult == ActualResult.KICK_SIX
                )
            }

        return offensiveTouchdowns + defensiveTouchdowns
    }

    private fun calculateAverageNormalPlayDiff(allPlays: List<Play>): Double {
        val average =
            allPlays
                .filter { play ->
                    play.playCall != PlayCall.KICKOFF_NORMAL &&
                        play.playCall != PlayCall.KICKOFF_ONSIDE &&
                        play.playCall != PlayCall.KICKOFF_SQUIB &&
                        play.playCall != PlayCall.PAT &&
                        play.playCall != PlayCall.TWO_POINT &&
                        play.playCall != PlayCall.KNEEL &&
                        play.playCall != PlayCall.SPIKE
                }
                .mapNotNull { it.difference }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateAverageSpecialTeamsDiff(allPlays: List<Play>): Double {
        val average =
            allPlays
                .filter { play ->
                    play.playCall == PlayCall.KICKOFF_NORMAL ||
                        play.playCall == PlayCall.KICKOFF_ONSIDE ||
                        play.playCall == PlayCall.KICKOFF_SQUIB ||
                        play.playCall == PlayCall.FIELD_GOAL ||
                        play.playCall == PlayCall.PUNT
                }
                .mapNotNull { it.difference }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateAverageYardsPerPlay(allPlays: List<Play>): Double {
        val average =
            allPlays
                .filter { play ->
                    play.playCall != PlayCall.KICKOFF_NORMAL &&
                        play.playCall != PlayCall.KICKOFF_ONSIDE &&
                        play.playCall != PlayCall.KICKOFF_SQUIB &&
                        play.playCall != PlayCall.PAT &&
                        play.playCall != PlayCall.TWO_POINT
                }
                .map { it.yards }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateFirstDowns(allPlays: List<Play>): Int {
        return allPlays.count { play -> play.actualResult == ActualResult.FIRST_DOWN }
    }

    private fun calculateThirdDownConversionSuccess(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.down == 3 && play.yards > play.yardsToGo
        }
    }

    private fun calculateThirdDownConversionAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.down == 3
        }
    }

    private fun calculateFourthDownConversionSuccess(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.down == 4 && play.yards > play.yardsToGo
        }
    }

    private fun calculateFourthDownConversionAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.down == 4 && (play.playCall == PlayCall.RUN || play.playCall == PlayCall.PASS)
        }
    }

    private fun calculateLargestLeadForHome(allPlays: List<Play>): Int {
        return allPlays.maxOfOrNull { play ->
            play.homeScore - play.awayScore
        } ?: 0
    }

    private fun calculateLargestDeficitForHome(allPlays: List<Play>): Int {
        return allPlays.maxOfOrNull { play ->
            play.awayScore - play.homeScore
        } ?: 0
    }

    private fun calculateLargestLeadForAway(allPlays: List<Play>): Int {
        return allPlays.maxOfOrNull { play ->
            play.awayScore - play.homeScore
        } ?: 0
    }

    private fun calculateLargestDeficitForAway(allPlays: List<Play>): Int {
        return allPlays.maxOfOrNull { play ->
            play.homeScore - play.awayScore
        } ?: 0
    }

    private fun calculatePassTouchdowns(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PASS && play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    private fun calculateRushTouchdowns(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.RUN && play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    private fun calculateRedZoneAttempts(
        allPlays: List<Play>,
        teamSide: TeamSide,
    ): Int {
        var redZoneAttempts = 0
        var isDriveInProgress = false
        var visitedRedZoneOnDrive = false

        allPlays.sortedBy { it.playId }.forEach { play ->
            when {
                // If the current play is a kickoff, end the current drive
                (
                    play.playCall == PlayCall.KICKOFF_NORMAL ||
                        play.playCall == PlayCall.KICKOFF_ONSIDE ||
                        play.playCall == PlayCall.KICKOFF_SQUIB
                ) && play.possession == teamSide -> {
                    isDriveInProgress = false
                }

                // Player starts or continues a drive (possession belongs to the player)
                play.possession == teamSide && !isDriveInProgress -> {
                    // Start a new drive
                    isDriveInProgress = true
                    visitedRedZoneOnDrive = false
                }

                // If possession changes to another player or a turnover happens
                play.possession != teamSide ||
                    play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN -> {
                    // End the current drive
                    isDriveInProgress = false
                }

                // If the current play is in the red zone
                play.ballLocation >= 80 &&
                    !visitedRedZoneOnDrive &&
                    isDriveInProgress &&
                    play.playCall != PlayCall.PAT -> {
                    // Increment the red zone attempts
                    redZoneAttempts++
                    visitedRedZoneOnDrive = true
                }
            }
        }
        return redZoneAttempts
    }

    private fun calculateRedZoneSuccesses(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.ballLocation >= 80 &&
                play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    private fun calculateAverageDiff(allPlays: List<Play>): Double {
        val average = allPlays.mapNotNull { it.difference }.average()
        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateTurnoverDifferential(
        turnoversLost: Int,
        turnoversForced: Int,
    ): Int {
        return turnoversForced - turnoversLost
    }

    private fun calculatePickSixes(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PASS && play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
        }
    }

    private fun calculateFumbleReturnTds(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.RUN && play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
        }
    }

    private fun calculateSafeties(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.actualResult == ActualResult.SAFETY
        }
    }

    private fun calculateAverageResponseSpeed(
        allPlays: List<Play>,
        teamSide: TeamSide,
    ): Double {
        val average =
            allPlays
                .filter { play ->
                    play.offensiveResponseSpeed != null && play.defensiveResponseSpeed != null
                }
                .map { play ->
                    if (play.possession == teamSide) {
                        play.offensiveResponseSpeed ?: 0L
                    } else {
                        play.defensiveResponseSpeed ?: 0L
                    }
                }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateQuarterScore(
        play: Play,
        currentQuarterScore: Int,
        possession: TeamSide,
    ): Int {
        if (play.possession == possession) {
            if (play.actualResult == ActualResult.TOUCHDOWN || play.actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
                play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN
            ) {
                return currentQuarterScore + 6
            }
            if (play.playCall == PlayCall.PAT && play.actualResult == ActualResult.GOOD) {
                return currentQuarterScore + 1
            }
            if (play.playCall == PlayCall.TWO_POINT && play.actualResult == ActualResult.GOOD) {
                return currentQuarterScore + 2
            }
            if (play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD) {
                return currentQuarterScore + 3
            }
        }
        if (play.possession != possession) {
            if (play.actualResult == ActualResult.TURNOVER_TOUCHDOWN || play.actualResult == ActualResult.RETURN_TOUCHDOWN ||
                play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN || play.actualResult == ActualResult.KICK_SIX
            ) {
                return currentQuarterScore + 6
            }
            if (play.playCall == PlayCall.PAT && play.actualResult == ActualResult.DEFENSE_TWO_POINT) {
                return currentQuarterScore + 1
            }
            if (play.playCall == PlayCall.TWO_POINT && play.actualResult == ActualResult.DEFENSE_TWO_POINT) {
                return currentQuarterScore + 2
            }
            if (play.actualResult == ActualResult.SAFETY) {
                return currentQuarterScore + 2
            }
        }
        return currentQuarterScore
    }

    private fun calculatePercentage(
        successes: Int,
        attempts: Int,
    ): Double {
        if (attempts == 0) {
            return 0.0
        }
        return (successes.toDouble() / attempts.toDouble()) * 100
    }

    /**
     * Get all game stats for a specific team and season
     * @param team Team name
     * @param season Season number
     * @return List of GameStats for the team in the specified season
     */
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
    ): List<com.fcfb.arceus.dto.EloHistoryEntry> {
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

            com.fcfb.arceus.dto.EloHistoryEntry(
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
}
