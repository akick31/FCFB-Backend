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
) {
    /**
     * Create a game stats entry
     * @param game
     */
    fun createGameStats(game: Game): List<GameStats> {
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

        // Iterate through the plays and update the game stats
        for (play in allPlays) {
            updateGameStats(
                game,
                allPlays,
                play,
            )
        }
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
     * Delete game stats entry by game ID
     */
    fun deleteByGameId(gameId: Int) = gameStatsRepository.deleteByGameId(gameId)

    /**
     * Calculate the stats for each team from the current play
     */
    private fun updateStatsForEachTeam(
        allPlays: List<Play>,
        play: Play,
        possession: TeamSide,
        game: Game,
        stats: GameStats,
        opponentStats: GameStats,
    ): List<GameStats> {
        val defendingTeam = if (possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME

        stats.score = game.homeScore
        stats.passAttempts =
            calculatePassAttempts(
                play, stats.passAttempts,
            )
        stats.passCompletions =
            calculatePassCompletions(
                play, stats.passCompletions,
            )
        stats.passCompletionPercentage =
            calculatePercentage(
                stats.passCompletions, stats.passAttempts,
            )
        stats.passYards =
            calculatePassYards(
                play, stats.passYards,
            )
        stats.longestPass =
            calculateLongestPass(
                play, stats.longestPass,
            )
        stats.sacksAllowed =
            calculateSacksAllowed(
                play, stats.sacksAllowed,
            )
        stats.sacksForced = opponentStats.sacksAllowed
        stats.rushAttempts =
            calculateRushAttempts(
                play, stats.rushAttempts,
            )
        stats.rushSuccesses =
            calculateRushSuccesses(
                play, stats.rushSuccesses,
            )
        stats.rushSuccessPercentage =
            calculatePercentage(
                stats.rushSuccesses, stats.rushAttempts,
            )
        stats.passSuccesses =
            calculatePassSuccesses(
                play, stats.passSuccesses,
            )
        stats.passSuccessPercentage =
            calculatePercentage(
                stats.passSuccesses, stats.passAttempts,
            )
        stats.rushYards =
            calculateRushYards(
                play, stats.rushYards,
            )
        stats.longestRun =
            calculateLongestRun(
                play, stats.longestRun,
            )
        stats.totalYards =
            calculateTotalYards(
                stats.passYards, stats.rushYards,
            )
        stats.interceptionsLost =
            calculateInterceptionsLost(
                play, stats.interceptionsLost,
            )
        stats.interceptionsForced = opponentStats.interceptionsLost
        stats.fumblesLost =
            calculateFumblesLost(
                play, stats.fumblesLost,
            )
        stats.fumblesForced = opponentStats.fumblesLost
        stats.turnoversLost =
            calculateTurnoversLost(
                stats.interceptionsLost, stats.fumblesLost,
            )
        stats.turnoversForced = opponentStats.turnoversLost
        stats.turnoverTouchdownsLost =
            calculateTurnoverTouchdownsLost(
                play, stats.turnoverTouchdownsLost,
            )
        stats.turnoverTouchdownsForced = opponentStats.turnoverTouchdownsLost
        stats.fieldGoalMade =
            calculateFieldGoalMade(
                play, stats.fieldGoalMade,
            )
        if (play.playCall == PlayCall.FIELD_GOAL && (play.result == Scenario.NO_GOOD || play.result == Scenario.BLOCKED_FIELD_GOAL)) {
            opponentStats.fieldGoalAttempts =
                calculateFieldGoalAttempts(
                    play, opponentStats.fieldGoalAttempts,
                )
        } else {
            stats.fieldGoalAttempts =
                calculateFieldGoalAttempts(
                    play, stats.fieldGoalAttempts,
                )
        }
        stats.fieldGoalPercentage =
            calculatePercentage(
                stats.fieldGoalMade, stats.fieldGoalAttempts,
            )
        stats.longestFieldGoal =
            calculateLongestFieldGoal(
                play, stats.longestFieldGoal,
            )
        opponentStats.blockedOpponentFieldGoals =
            calculateBlockedOpponentFieldGoals(
                play, opponentStats.blockedOpponentFieldGoals,
            )
        stats.fieldGoalTouchdown =
            calculateFieldGoalTouchdown(
                play, stats.fieldGoalTouchdown,
            )
        opponentStats.puntsAttempted =
            calculatePuntsAttempted(
                play, opponentStats.puntsAttempted,
            )
        opponentStats.longestPunt =
            calculateLongestPunt(
                play, opponentStats.longestPunt,
            )
        stats.averagePuntLength =
            calculateAveragePuntLength(
                allPlays, possession,
            )
        opponentStats.blockedOpponentPunt =
            calculateBlockedOpponentPunt(
                play, opponentStats.blockedOpponentPunt,
            )
        opponentStats.puntReturnTd =
            calculatePuntReturnTd(
                play, opponentStats.puntReturnTd,
            )
        opponentStats.puntReturnTdPercentage =
            calculatePercentage(
                opponentStats.puntReturnTd, opponentStats.puntsAttempted,
            )
        opponentStats.numberOfKickoffs =
            calculateNumberOfKickoffs(
                play, opponentStats.numberOfKickoffs,
            )
        opponentStats.onsideAttempts =
            calculateOnsideAttempts(
                play, opponentStats.onsideAttempts,
            )
        opponentStats.onsideSuccess =
            calculateOnsideSuccess(
                play, opponentStats.onsideSuccess,
            )
        opponentStats.onsideSuccessPercentage =
            calculatePercentage(
                opponentStats.onsideSuccess, opponentStats.onsideAttempts,
            )
        opponentStats.normalKickoffAttempts =
            calculateNormalKickoffAttempts(
                play, opponentStats.normalKickoffAttempts,
            )
        opponentStats.touchbacks =
            calculateTouchbacks(
                play, opponentStats.touchbacks,
            )
        opponentStats.touchbackPercentage =
            calculatePercentage(
                opponentStats.touchbacks, opponentStats.normalKickoffAttempts,
            )
        opponentStats.kickReturnTd =
            calculateKickReturnTd(
                play, opponentStats.kickReturnTd,
            )
        opponentStats.kickReturnTdPercentage =
            calculatePercentage(
                opponentStats.kickReturnTd, opponentStats.numberOfKickoffs,
            )
        stats.numberOfDrives =
            calculateNumberOfDrives(
                allPlays, possession,
            )
        stats.timeOfPossession =
            calculateTimeOfPossession(
                allPlays, possession,
            )
        stats.touchdowns =
            calculateTouchdowns(
                allPlays, possession,
            )
        stats.averageOffensiveDiff =
            calculateAverageOffensiveDiff(
                allPlays, possession,
            )
        stats.averageDefensiveDiff =
            calculateAverageDefensiveDiff(
                allPlays, possession,
            )
        stats.averageOffensiveSpecialTeamsDiff =
            calculateAverageOffensiveSpecialTeamsDiff(
                allPlays, possession,
            )
        stats.averageDefensiveSpecialTeamsDiff =
            calculateAverageDefensiveSpecialTeamsDiff(
                allPlays, possession,
            )
        stats.averageYardsPerPlay =
            calculateAverageYardsPerPlay(
                allPlays, possession,
            )
        stats.firstDowns =
            calculateFirstDowns(
                allPlays, possession,
            )
        stats.thirdDownConversionSuccess =
            calculateThirdDownConversionSuccess(
                play, stats.thirdDownConversionSuccess,
            )
        stats.thirdDownConversionAttempts =
            calculateThirdDownConversionAttempts(
                play, stats.thirdDownConversionAttempts,
            )
        stats.thirdDownConversionPercentage =
            calculatePercentage(
                stats.thirdDownConversionSuccess, stats.thirdDownConversionAttempts,
            )
        stats.fourthDownConversionSuccess =
            calculateFourthDownConversionSuccess(
                play, stats.fourthDownConversionSuccess,
            )
        stats.fourthDownConversionAttempts =
            calculateFourthDownConversionAttempts(
                play, stats.fourthDownConversionAttempts,
            )
        stats.fourthDownConversionPercentage =
            calculatePercentage(
                stats.fourthDownConversionSuccess, stats.fourthDownConversionAttempts,
            )
        if (play.possession == TeamSide.HOME) {
            stats.largestLead = calculateLargestLeadForHome(play, stats.largestLead)
            stats.largestDeficit = calculateLargestDeficitForHome(play, stats.largestDeficit)
        } else {
            stats.largestLead = calculateLargestLeadForAway(play, stats.largestLead)
            stats.largestDeficit = calculateLargestDeficitForAway(play, stats.largestDeficit)
        }
        stats.passTouchdowns =
            calculatePassTouchdowns(
                play, stats.passTouchdowns,
            )
        stats.rushTouchdowns =
            calculateRushTouchdowns(
                play, stats.rushTouchdowns,
            )
        stats.redZoneAttempts =
            calculateRedZoneAttempts(
                allPlays, possession,
            )
        stats.redZoneSuccesses =
            calculateRedZoneSuccesses(
                allPlays, possession,
            )
        stats.redZoneSuccessPercentage =
            calculatePercentage(
                stats.redZoneSuccesses, stats.redZoneAttempts,
            )
        stats.redZonePercentage =
            calculatePercentage(
                stats.redZoneAttempts, stats.numberOfDrives,
            )
        stats.turnoverDifferential =
            calculateTurnoverDifferential(
                stats.turnoversLost, stats.turnoversForced,
            )
        stats.pickSixesThrown =
            calculatePickSixes(
                play, stats.pickSixesThrown,
            )
        opponentStats.pickSixesForced = stats.pickSixesThrown
        stats.fumbleReturnTdsCommitted =
            calculateFumbleReturnTds(
                play, stats.fumbleReturnTdsCommitted,
            )
        opponentStats.fumbleReturnTdsForced = stats.fumbleReturnTdsCommitted
        stats.safetiesCommitted =
            calculateSafeties(
                play, stats.safetiesCommitted,
            )
        opponentStats.safetiesForced = stats.safetiesCommitted
        stats.averageResponseSpeed =
            calculateAverageResponseSpeed(
                allPlays, possession,
            )
        opponentStats.averageResponseSpeed =
            calculateAverageResponseSpeed(
                allPlays, defendingTeam,
            )
        stats.gameStatus = game.gameStatus
        opponentStats.gameStatus = game.gameStatus
        stats.averageDiff = calculateAverageDiff(allPlays)
        opponentStats.averageDiff = calculateAverageDiff(allPlays)
        stats.lastModifiedTs =
            ZonedDateTime.now(
                ZoneId.of("America/New_York"),
            ).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        opponentStats.lastModifiedTs =
            ZonedDateTime.now(
                ZoneId.of("America/New_York"),
            ).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        gameStatsRepository.save(stats)
        gameStatsRepository.save(opponentStats)
        return listOf(stats, opponentStats)
    }

    private fun updateScoreStats(
        play: Play,
        stats: GameStats,
        opponentStats: GameStats,
        possession: TeamSide,
    ) {
        val defendingTeam = if (possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
        if (play.quarter == 1) {
            stats.q1Score = calculateQuarterScore(play, stats.q1Score, possession)
            opponentStats.q1Score = calculateQuarterScore(play, opponentStats.q1Score, defendingTeam)
        }
        if (play.quarter == 2) {
            stats.q2Score = calculateQuarterScore(play, stats.q2Score, possession)
            opponentStats.q2Score = calculateQuarterScore(play, opponentStats.q2Score, defendingTeam)
        }
        if (play.quarter == 3) {
            stats.q3Score = calculateQuarterScore(play, stats.q3Score, possession)
            opponentStats.q3Score = calculateQuarterScore(play, opponentStats.q3Score, defendingTeam)
        }
        if (play.quarter == 4) {
            stats.q4Score = calculateQuarterScore(play, stats.q4Score, possession)
            opponentStats.q4Score = calculateQuarterScore(play, opponentStats.q4Score, defendingTeam)
        }
        if (play.quarter == 5) {
            stats.otScore = calculateQuarterScore(play, stats.otScore, possession)
            opponentStats.otScore = calculateQuarterScore(play, opponentStats.otScore, defendingTeam)
        }
    }

    /**
     * Update the game stats for the current game
     */
    fun updateGameStats(
        game: Game,
        allPlays: List<Play>,
        play: Play,
    ): List<GameStats> {
        if (play.possession == TeamSide.HOME) {
            val stats = getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
            val opponentStats = getGameStatsByIdAndTeam(game.gameId, game.awayTeam)
            updateScoreStats(play, stats, opponentStats, play.possession)
            return updateStatsForEachTeam(allPlays, play, TeamSide.HOME, game, stats, opponentStats)
        } else {
            val stats = getGameStatsByIdAndTeam(game.gameId, game.awayTeam)
            val opponentStats = getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
            updateScoreStats(play, stats, opponentStats, play.possession)
            return updateStatsForEachTeam(allPlays, play, TeamSide.AWAY, game, stats, opponentStats)
        }
    }

    private fun calculatePassAttempts(
        play: Play,
        currentPassAttempts: Int,
    ): Int {
        // Don't count sacks as pass attempts
        if (play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS) {
            return currentPassAttempts
        }
        if (play.playCall == PlayCall.SPIKE) {
            return currentPassAttempts + 1
        }
        if (play.playCall == PlayCall.PASS) {
            return currentPassAttempts + 1
        }
        return currentPassAttempts
    }

    private fun calculatePassCompletions(
        play: Play,
        currentPassCompletions: Int,
    ): Int {
        if (play.playCall == PlayCall.PASS && (
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
            )
        ) {
            return currentPassCompletions + 1
        }
        return currentPassCompletions
    }

    private fun calculatePassYards(
        play: Play,
        currentPassYards: Int,
    ): Int {
        // Don't count sacks towards passing yards
        if (play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS) {
            return currentPassYards
        }
        if (play.playCall == PlayCall.PASS) {
            return currentPassYards + (play.yards)
        }
        return currentPassYards
    }

    private fun calculateLongestPass(
        play: Play,
        currentLongestPass: Int,
    ): Int {
        if (play.playCall == PlayCall.PASS && play.result != Scenario.INCOMPLETE) {
            return if (play.yards > currentLongestPass) play.yards else currentLongestPass
        }
        return currentLongestPass
    }

    private fun calculateSacksAllowed(
        play: Play,
        currentSacksAllowed: Int,
    ): Int {
        if (play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS) {
            return currentSacksAllowed + 1
        }
        return currentSacksAllowed
    }

    private fun calculateRushAttempts(
        play: Play,
        currentRushAttempts: Int,
    ): Int {
        if (play.playCall == PlayCall.RUN) {
            return currentRushAttempts + 1
        }
        return currentRushAttempts
    }

    private fun calculateRushSuccesses(
        play: Play,
        currentRushSuccesses: Int,
    ): Int {
        if (play.playCall != PlayCall.RUN) return currentRushSuccesses

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

        return if (isSuccess || play.actualResult == ActualResult.TOUCHDOWN) {
            currentRushSuccesses + 1
        } else {
            currentRushSuccesses
        }
    }

    private fun calculateRushYards(
        play: Play,
        currentRushYards: Int,
    ): Int {
        // Count sacks towards rushing yards
        if (play.playCall == PlayCall.PASS && (
                play.result == Scenario.LOSS_OF_10_YARDS ||
                    play.result == Scenario.LOSS_OF_7_YARDS ||
                    play.result == Scenario.LOSS_OF_5_YARDS ||
                    play.result == Scenario.LOSS_OF_3_YARDS ||
                    play.result == Scenario.LOSS_OF_2_YARDS ||
                    play.result == Scenario.LOSS_OF_1_YARD
            )
        ) {
            return currentRushYards + (play.yards)
        }
        if (play.playCall == PlayCall.RUN && (
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
            )
        ) {
            return currentRushYards + (play.yards)
        }
        return currentRushYards
    }

    private fun calculateLongestRun(
        play: Play,
        currentLongestRun: Int,
    ): Int {
        if (play.playCall == PlayCall.RUN && play.result != Scenario.INCOMPLETE) {
            return if (play.yards > currentLongestRun) play.yards else currentLongestRun
        }
        return currentLongestRun
    }

    private fun calculatePassSuccesses(
        play: Play,
        currentPassSuccesses: Int,
    ): Int {
        if (play.playCall != PlayCall.PASS) return currentPassSuccesses

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

        return if (isSuccess || play.actualResult == ActualResult.TOUCHDOWN) {
            currentPassSuccesses + 1
        } else {
            currentPassSuccesses
        }
    }

    private fun calculateTotalYards(
        passingYards: Int,
        rushingYards: Int,
    ): Int {
        return passingYards + rushingYards
    }

    private fun calculateInterceptionsLost(
        play: Play,
        currentInterceptionsLost: Int,
    ): Int {
        if (play.playCall == PlayCall.PASS && (
                play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
            )
        ) {
            return currentInterceptionsLost + 1
        }
        return currentInterceptionsLost
    }

    private fun calculateFumblesLost(
        play: Play,
        currentFumblesLost: Int,
    ): Int {
        if (play.playCall == PlayCall.RUN &&
            (
                play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
            )
        ) {
            return currentFumblesLost + 1
        }
        return currentFumblesLost
    }

    private fun calculateTurnoversLost(
        interceptionsLost: Int,
        fumblesLost: Int,
    ): Int {
        return interceptionsLost + fumblesLost
    }

    private fun calculateTurnoverTouchdownsLost(
        play: Play,
        currentTurnoverTouchdownsLost: Int,
    ): Int {
        if (play.actualResult == ActualResult.TURNOVER_TOUCHDOWN) {
            return currentTurnoverTouchdownsLost + 1
        }
        return currentTurnoverTouchdownsLost
    }

    private fun calculateFieldGoalMade(
        play: Play,
        currentFieldGoalMade: Int,
    ): Int {
        if (play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD) {
            return currentFieldGoalMade + 1
        }
        return currentFieldGoalMade
    }

    private fun calculateFieldGoalAttempts(
        play: Play,
        currentFieldGoalAttempts: Int,
    ): Int {
        if (play.playCall == PlayCall.FIELD_GOAL) {
            return currentFieldGoalAttempts + 1
        }
        return currentFieldGoalAttempts
    }

    private fun calculateLongestFieldGoal(
        play: Play,
        currentLongestFieldGoal: Int,
    ): Int {
        if (play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD && (
                ((100 - play.ballLocation) + 17) > currentLongestFieldGoal
            )
        ) {
            return ((100 - play.ballLocation)) + 17
        }
        return currentLongestFieldGoal
    }

    private fun calculateBlockedOpponentFieldGoals(
        play: Play,
        currentBlockedOpponentFieldGoals: Int,
    ): Int {
        if (play.playCall == PlayCall.FIELD_GOAL && play.actualResult == ActualResult.BLOCKED) {
            return currentBlockedOpponentFieldGoals + 1
        }
        return currentBlockedOpponentFieldGoals
    }

    private fun calculateFieldGoalTouchdown(
        play: Play,
        currentFieldGoalTouchdown: Int,
    ): Int {
        if (play.playCall == PlayCall.FIELD_GOAL && play.actualResult == ActualResult.KICK_SIX) {
            return currentFieldGoalTouchdown + 1
        }
        return currentFieldGoalTouchdown
    }

    private fun calculatePuntsAttempted(
        play: Play,
        currentPuntsAttempted: Int,
    ): Int {
        if (play.playCall == PlayCall.PUNT) {
            return currentPuntsAttempted + 1
        }
        return currentPuntsAttempted
    }

    private fun calculateLongestPunt(
        play: Play,
        currentLongestPunt: Int,
    ): Int {
        val puntDisances =
            listOf(
                Scenario.FIVE_YARD_PUNT, Scenario.TEN_YARD_PUNT, Scenario.FIFTEEN_YARD_PUNT, Scenario.TWENTY_YARD_PUNT,
                Scenario.TWENTY_FIVE_YARD_PUNT, Scenario.THIRTY_YARD_PUNT, Scenario.THIRTY_FIVE_YARD_PUNT,
                Scenario.FORTY_YARD_PUNT, Scenario.FORTY_FIVE_YARD_PUNT, Scenario.FIFTY_YARD_PUNT,
                Scenario.FIFTY_FIVE_YARD_PUNT, Scenario.SIXTY_YARD_PUNT, Scenario.SIXTY_FIVE_YARD_PUNT,
                Scenario.SEVENTY_YARD_PUNT,
            )
        if (play.result in puntDisances) {
            val puntDistance = play.result?.description?.substringBefore(" YARD PUNT")?.toInt() ?: 0
            if ((puntDistance) > currentLongestPunt) {
                return puntDistance
            }
        }
        return currentLongestPunt
    }

    private fun calculateAveragePuntLength(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Double {
        val average =
            allPlays.filter {
                it.playCall == PlayCall.PUNT && it.possession == possession &&
                    it.result?.description?.contains(" YARD PUNT") ?: false
            }.map {
                it.result?.description?.substringBefore(" YARD PUNT")?.toInt() ?: 0
            }.average()
        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateBlockedOpponentPunt(
        play: Play,
        currentBlockedOpponentPunt: Int,
    ): Int {
        if (play.playCall == PlayCall.PUNT && play.actualResult == ActualResult.BLOCKED) {
            return currentBlockedOpponentPunt + 1
        }
        return currentBlockedOpponentPunt
    }

    private fun calculatePuntReturnTd(
        play: Play,
        currentPuntReturnTd: Int,
    ): Int {
        if (play.playCall == PlayCall.PUNT && play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN) {
            return currentPuntReturnTd + 1
        }
        return currentPuntReturnTd
    }

    private fun calculateNumberOfKickoffs(
        play: Play,
        currentNumberOfKickoffs: Int,
    ): Int {
        if (play.playCall == PlayCall.KICKOFF_NORMAL ||
            play.playCall == PlayCall.KICKOFF_ONSIDE ||
            play.playCall == PlayCall.KICKOFF_SQUIB
        ) {
            return currentNumberOfKickoffs + 1
        }
        return currentNumberOfKickoffs
    }

    private fun calculateOnsideAttempts(
        play: Play,
        currentOnsideAttempts: Int,
    ): Int {
        if (play.playCall == PlayCall.KICKOFF_ONSIDE) {
            return currentOnsideAttempts + 1
        }
        return currentOnsideAttempts
    }

    private fun calculateOnsideSuccess(
        play: Play,
        currentOnsideSuccess: Int,
    ): Int {
        if (play.playCall == PlayCall.KICKOFF_ONSIDE && play.result == Scenario.RECOVERED) {
            return currentOnsideSuccess + 1
        }
        return currentOnsideSuccess
    }

    private fun calculateNormalKickoffAttempts(
        play: Play,
        currentNormalKickoffAttempts: Int,
    ): Int {
        if (play.playCall == PlayCall.KICKOFF_NORMAL) {
            return currentNormalKickoffAttempts + 1
        }
        return currentNormalKickoffAttempts
    }

    private fun calculateTouchbacks(
        play: Play,
        currentTouchbacks: Int,
    ): Int {
        if (play.playCall == PlayCall.KICKOFF_NORMAL && play.result == Scenario.TOUCHBACK) {
            return currentTouchbacks + 1
        }
        return currentTouchbacks
    }

    private fun calculateKickReturnTd(
        play: Play,
        currentKickReturnTd: Int,
    ): Int {
        if (play.playCall == PlayCall.KICKOFF_NORMAL && play.actualResult == ActualResult.RETURN_TOUCHDOWN) {
            return currentKickReturnTd + 1
        }
        return currentKickReturnTd
    }

    private fun calculateNumberOfDrives(
        allPlays: List<Play>,
        possession: TeamSide,
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
                ) && play.possession == possession -> {
                    isDriveInProgress = false
                }

                // Player starts or continues a drive (possession belongs to the player)
                play.possession == possession && !isDriveInProgress -> {
                    // Start a new drive
                    driveCount++
                    isDriveInProgress = true
                }

                // If possession changes to another player or a turnover happens
                play.possession != possession ||
                    play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN -> {
                    // End the current drive
                    isDriveInProgress = false
                }
            }
        }

        return driveCount
    }

    private fun calculateTimeOfPossession(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Int {
        return allPlays.filter {
            it.possession == possession &&
                it.playCall != PlayCall.KICKOFF_NORMAL &&
                it.playCall != PlayCall.KICKOFF_ONSIDE &&
                it.playCall != PlayCall.KICKOFF_SQUIB
        }.sumOf {
            if (it.clock - (it.playTime) + (it.runoffTime) < 0) {
                it.clock
            } else {
                it.playTime + it.runoffTime
            }
        }
    }

    private fun calculateTouchdowns(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Int {
        val offensiveTouchdowns =
            allPlays.count {
                it.possession == possession &&
                    (
                        it.actualResult == ActualResult.TOUCHDOWN ||
                            it.actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
                            it.actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN
                    )
            }
        val defensiveTouchdowns =
            allPlays.count {
                it.possession != possession &&
                    (
                        it.actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
                            it.actualResult == ActualResult.RETURN_TOUCHDOWN ||
                            it.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
                            it.actualResult == ActualResult.KICK_SIX
                    )
            }
        return offensiveTouchdowns + defensiveTouchdowns
    }

    private fun calculateAverageOffensiveDiff(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Double {
        val average =
            allPlays.filter {
                it.possession == possession &&
                    it.playCall != PlayCall.KICKOFF_NORMAL &&
                    it.playCall != PlayCall.KICKOFF_ONSIDE &&
                    it.playCall != PlayCall.KICKOFF_SQUIB &&
                    it.playCall != PlayCall.PAT &&
                    it.playCall != PlayCall.TWO_POINT &&
                    it.playCall != PlayCall.KNEEL &&
                    it.playCall != PlayCall.SPIKE
            }.mapNotNull { it.difference }.average()
        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateAverageDefensiveDiff(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Double {
        val average =
            allPlays.filter {
                it.possession != possession &&
                    it.playCall != PlayCall.KICKOFF_NORMAL &&
                    it.playCall != PlayCall.KICKOFF_ONSIDE &&
                    it.playCall != PlayCall.KICKOFF_SQUIB &&
                    it.playCall != PlayCall.PAT &&
                    it.playCall != PlayCall.TWO_POINT &&
                    it.playCall != PlayCall.KNEEL &&
                    it.playCall != PlayCall.SPIKE
            }.mapNotNull { it.difference }.average()
        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateAverageOffensiveSpecialTeamsDiff(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Double {
        val average =
            allPlays.filter {
                it.possession == possession &&
                    (
                        it.playCall == PlayCall.KICKOFF_NORMAL ||
                            it.playCall == PlayCall.KICKOFF_ONSIDE ||
                            it.playCall == PlayCall.KICKOFF_SQUIB ||
                            it.playCall == PlayCall.FIELD_GOAL ||
                            it.playCall == PlayCall.PUNT
                    )
            }.mapNotNull { it.difference }.average()
        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateAverageDefensiveSpecialTeamsDiff(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Double {
        val average =
            allPlays.filter {
                it.possession != possession &&
                    (
                        it.playCall == PlayCall.KICKOFF_NORMAL ||
                            it.playCall == PlayCall.KICKOFF_ONSIDE ||
                            it.playCall == PlayCall.KICKOFF_SQUIB ||
                            it.playCall == PlayCall.FIELD_GOAL ||
                            it.playCall == PlayCall.PUNT
                    )
            }.mapNotNull { it.difference }.average()
        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateAverageYardsPerPlay(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Double {
        val average =
            allPlays.filter {
                it.possession == possession &&
                    it.playCall != PlayCall.KICKOFF_NORMAL &&
                    it.playCall != PlayCall.KICKOFF_ONSIDE &&
                    it.playCall != PlayCall.KICKOFF_SQUIB &&
                    it.playCall != PlayCall.PAT &&
                    it.playCall != PlayCall.TWO_POINT
            }.map { it.yards }.average()
        return if (average.isNaN()) 0.0 else average
    }

    private fun calculateFirstDowns(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Int {
        return allPlays.count {
            it.possession == possession &&
                it.actualResult == ActualResult.FIRST_DOWN
        }
    }

    private fun calculateThirdDownConversionSuccess(
        play: Play,
        currentThirdDownConversionSuccess: Int,
    ): Int {
        if (play.down == 3 && (play.yards) > (play.yardsToGo)) {
            return currentThirdDownConversionSuccess + 1
        }
        return currentThirdDownConversionSuccess
    }

    private fun calculateThirdDownConversionAttempts(
        play: Play,
        currentThirdDownConversionAttempts: Int,
    ): Int {
        if (play.down == 3) {
            return currentThirdDownConversionAttempts + 1
        }
        return currentThirdDownConversionAttempts
    }

    private fun calculateFourthDownConversionSuccess(
        play: Play,
        currentFourthDownConversionSuccess: Int,
    ): Int {
        if (play.down == 4 && (play.yards) > (play.yardsToGo)) {
            return currentFourthDownConversionSuccess + 1
        }
        return currentFourthDownConversionSuccess
    }

    private fun calculateFourthDownConversionAttempts(
        play: Play,
        currentFourthDownConversionAttempts: Int,
    ): Int {
        if (play.down == 4 && (play.playCall == PlayCall.RUN || play.playCall == PlayCall.PASS)) {
            return currentFourthDownConversionAttempts + 1
        }
        return currentFourthDownConversionAttempts
    }

    private fun calculateLargestLeadForHome(
        play: Play,
        currentLargestLead: Int,
    ): Int {
        val lead = play.homeScore - play.awayScore
        return if (lead > currentLargestLead) lead else currentLargestLead
    }

    private fun calculateLargestDeficitForHome(
        play: Play,
        currentLargestDeficit: Int,
    ): Int {
        val deficit = play.awayScore - play.homeScore
        return if (deficit > currentLargestDeficit) deficit else currentLargestDeficit
    }

    private fun calculateLargestLeadForAway(
        play: Play,
        currentLargestLead: Int,
    ): Int {
        val lead = play.awayScore - play.homeScore
        return if (lead > currentLargestLead) lead else currentLargestLead
    }

    private fun calculateLargestDeficitForAway(
        play: Play,
        currentLargestDeficit: Int,
    ): Int {
        val deficit = play.homeScore - play.awayScore
        return if (deficit > currentLargestDeficit) deficit else currentLargestDeficit
    }

    private fun calculatePassTouchdowns(
        play: Play,
        currentPassTouchdowns: Int,
    ): Int {
        if (play.playCall == PlayCall.PASS && play.actualResult == ActualResult.TOUCHDOWN) {
            return currentPassTouchdowns + 1
        }
        return currentPassTouchdowns
    }

    private fun calculateRushTouchdowns(
        play: Play,
        currentRushTouchdowns: Int,
    ): Int {
        if (play.playCall == PlayCall.RUN && play.actualResult == ActualResult.TOUCHDOWN) {
            return currentRushTouchdowns + 1
        }
        return currentRushTouchdowns
    }

    private fun calculateRedZoneAttempts(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Int {
        var redZoneAttempts = 0
        var driveCount = 0
        var isDriveInProgress = false
        var visitedRedZoneOnDrive = false

        allPlays.sortedBy { it.playId }.forEach { play ->
            when {
                // If the current play is a kickoff, end the current drive
                (
                    play.playCall == PlayCall.KICKOFF_NORMAL ||
                        play.playCall == PlayCall.KICKOFF_ONSIDE ||
                        play.playCall == PlayCall.KICKOFF_SQUIB
                ) && play.possession == possession -> {
                    isDriveInProgress = false
                }

                // Player starts or continues a drive (possession belongs to the player)
                play.possession == possession && !isDriveInProgress -> {
                    // Start a new drive
                    driveCount++
                    isDriveInProgress = true
                    visitedRedZoneOnDrive = false
                }

                // If possession changes to another player or a turnover happens
                play.possession != possession ||
                    play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN -> {
                    // End the current drive
                    isDriveInProgress = false
                }

                // If the current play is in the red zone
                (play.ballLocation) >= 80 &&
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

    private fun calculateRedZoneSuccesses(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Int {
        return allPlays.count {
            it.possession == possession &&
                (it.ballLocation) >= 80 &&
                it.actualResult == ActualResult.TOUCHDOWN
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

    private fun calculatePickSixes(
        play: Play,
        currentPickSixes: Int,
    ): Int {
        if (play.playCall == PlayCall.PASS && play.actualResult == ActualResult.TURNOVER_TOUCHDOWN) {
            return currentPickSixes + 1
        }
        return currentPickSixes
    }

    private fun calculateFumbleReturnTds(
        play: Play,
        currentFumbleReturnTds: Int,
    ): Int {
        if (play.playCall == PlayCall.RUN && play.actualResult == ActualResult.TURNOVER_TOUCHDOWN) {
            return currentFumbleReturnTds + 1
        }
        return currentFumbleReturnTds
    }

    private fun calculateSafeties(
        play: Play,
        currentSafeties: Int,
    ): Int {
        if (play.actualResult == ActualResult.SAFETY) {
            return currentSafeties + 1
        }
        return currentSafeties
    }

    private fun calculateAverageResponseSpeed(
        allPlays: List<Play>,
        possession: TeamSide,
    ): Double {
        val average =
            allPlays
                .filter { play ->
                    play.offensiveResponseSpeed != null && play.defensiveResponseSpeed != null
                }
                .map { play ->
                    if (play.possession == possession) {
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
}
