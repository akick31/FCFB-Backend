package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Shared stat classification sets and stat-reading helpers used across the record
 * generation/checking services (game, season, and general records).
 */
@Service
class RecordStatUtils(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
) {
    /**
     * Stats that should ONLY track lowest values (lower is better)
     */
    val lowestOnlyStats =
        setOf(
            Stats.AVERAGE_OFFENSIVE_DIFF,
            Stats.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_RESPONSE_SPEED,
        )

    /**
     * Stats that should track both highest and lowest values
     */
    val dualRecordStats =
        setOf(
            // Performance metrics (defensive diff and average diff can be both highest and lowest)
            Stats.AVERAGE_DEFENSIVE_DIFF,
            Stats.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_DIFF,
            // Game flow stats
            Stats.TIME_OF_POSSESSION,
            // Total offense stats
            Stats.TOTAL_YARDS,
            Stats.AVERAGE_YARDS_PER_PLAY,
            Stats.FIRST_DOWNS,
            // Passing stats (except longest)
            Stats.PASS_ATTEMPTS,
            Stats.PASS_COMPLETIONS,
            Stats.PASS_YARDS,
            Stats.PASS_TOUCHDOWNS,
            Stats.PASS_SUCCESSES,
            // Rushing stats (except longest)
            Stats.RUSH_ATTEMPTS,
            Stats.RUSH_SUCCESSES,
            Stats.RUSH_YARDS,
            Stats.RUSH_TOUCHDOWNS,
            // Down conversions
            Stats.THIRD_DOWN_CONVERSION_SUCCESS,
            Stats.THIRD_DOWN_CONVERSION_ATTEMPTS,
            Stats.FOURTH_DOWN_CONVERSION_SUCCESS,
            Stats.FOURTH_DOWN_CONVERSION_ATTEMPTS,
            // Red zone
            Stats.RED_ZONE_ATTEMPTS,
            Stats.RED_ZONE_SUCCESSES,
            // Touchdowns
            Stats.TOUCHDOWNS,
            // Kickoffs
            Stats.NUMBER_OF_KICKOFFS,
            Stats.ONSIDE_ATTEMPTS,
            Stats.ONSIDE_SUCCESS,
            Stats.NORMAL_KICKOFF_ATTEMPTS,
            Stats.TOUCHBACKS,
            Stats.KICK_RETURN_TD,
        )

    /**
     * Stats that are general records (don't need season/game distinction)
     * These are things like "longest field goal ever" or "fastest response time ever"
     */
    val generalRecordStats =
        setOf(
            Stats.LONGEST_PASS,
            Stats.LONGEST_RUN,
            Stats.LONGEST_FIELD_GOAL,
            Stats.LONGEST_PUNT,
        )

    /**
     * Stats that are GAME-specific only (not applicable to season records)
     * These are things like time of possession, number of drives, quarter scores, score
     */
    val gameOnlyStats =
        setOf(
            Stats.TIME_OF_POSSESSION,
            Stats.NUMBER_OF_DRIVES,
            Stats.Q1_SCORE,
            Stats.Q2_SCORE,
            Stats.Q3_SCORE,
            Stats.Q4_SCORE,
            Stats.OT_SCORE,
            Stats.SCORE,
        )

    /**
     * Get the value of a specific stat from GameStats using reflection
     */
    fun getStatValue(
        statName: Stats,
        gameStats: GameStats,
    ): Double {
        val propertyName =
            when (statName) {
                // Basic Game Info
                Stats.SCORE -> "score"

                // Passing Stats
                Stats.PASS_ATTEMPTS -> "passAttempts"
                Stats.PASS_COMPLETIONS -> "passCompletions"
                Stats.PASS_COMPLETION_PERCENTAGE -> "passCompletionPercentage"
                Stats.PASS_YARDS -> "passYards"
                Stats.LONGEST_PASS -> "longestPass"
                Stats.PASS_TOUCHDOWNS -> "passTouchdowns"
                Stats.PASS_SUCCESSES -> "passSuccesses"
                Stats.PASS_SUCCESS_PERCENTAGE -> "passSuccessPercentage"

                // Rushing Stats
                Stats.RUSH_ATTEMPTS -> "rushAttempts"
                Stats.RUSH_SUCCESSES -> "rushSuccesses"
                Stats.RUSH_SUCCESS_PERCENTAGE -> "rushSuccessPercentage"
                Stats.RUSH_YARDS -> "rushYards"
                Stats.LONGEST_RUN -> "longestRun"
                Stats.RUSH_TOUCHDOWNS -> "rushTouchdowns"

                // Total Offense
                Stats.TOTAL_YARDS -> "totalYards"
                Stats.AVERAGE_YARDS_PER_PLAY -> "averageYardsPerPlay"
                Stats.FIRST_DOWNS -> "firstDowns"

                // Sacks
                Stats.SACKS_ALLOWED -> "sacksAllowed"
                Stats.SACKS_FORCED -> "sacksForced"

                // Turnovers
                Stats.INTERCEPTIONS_LOST -> "interceptionsLost"
                Stats.INTERCEPTIONS_FORCED -> "interceptionsForced"
                Stats.FUMBLES_LOST -> "fumblesLost"
                Stats.FUMBLES_FORCED -> "fumblesForced"
                Stats.TURNOVERS_LOST -> "turnoversLost"
                Stats.TURNOVERS_FORCED -> "turnoversForced"
                Stats.TURNOVER_DIFFERENTIAL -> "turnoverDifferential"
                Stats.TURNOVER_TOUCHDOWNS_LOST -> "turnoverTouchdownsLost"
                Stats.TURNOVER_TOUCHDOWNS_FORCED -> "turnoverTouchdownsForced"
                Stats.PICK_SIXES_THROWN -> "pickSixesThrown"
                Stats.PICK_SIXES_FORCED -> "pickSixesForced"
                Stats.FUMBLE_RETURN_TDS_COMMITTED -> "fumbleReturnTdsCommitted"
                Stats.FUMBLE_RETURN_TDS_FORCED -> "fumbleReturnTdsForced"

                // Field Goals
                Stats.FIELD_GOAL_MADE -> "fieldGoalMade"
                Stats.FIELD_GOAL_ATTEMPTS -> "fieldGoalAttempts"
                Stats.FIELD_GOAL_PERCENTAGE -> "fieldGoalPercentage"
                Stats.LONGEST_FIELD_GOAL -> "longestFieldGoal"
                Stats.BLOCKED_OPPONENT_FIELD_GOALS -> "blockedOpponentFieldGoals"
                Stats.FIELD_GOAL_TOUCHDOWN -> "fieldGoalTouchdown"

                // Punting
                Stats.PUNTS_ATTEMPTED -> "puntsAttempted"
                Stats.LONGEST_PUNT -> "longestPunt"
                Stats.AVERAGE_PUNT_LENGTH -> "averagePuntLength"
                Stats.BLOCKED_OPPONENT_PUNT -> "blockedOpponentPunt"
                Stats.PUNT_RETURN_TD -> "puntReturnTd"
                Stats.PUNT_RETURN_TD_PERCENTAGE -> "puntReturnTdPercentage"

                // Kickoffs
                Stats.NUMBER_OF_KICKOFFS -> "numberOfKickoffs"
                Stats.ONSIDE_ATTEMPTS -> "onsideAttempts"
                Stats.ONSIDE_SUCCESS -> "onsideSuccess"
                Stats.ONSIDE_SUCCESS_PERCENTAGE -> "onsideSuccessPercentage"
                Stats.NORMAL_KICKOFF_ATTEMPTS -> "normalKickoffAttempts"
                Stats.TOUCHBACKS -> "touchbacks"
                Stats.TOUCHBACK_PERCENTAGE -> "touchbackPercentage"
                Stats.KICK_RETURN_TD -> "kickReturnTd"
                Stats.KICK_RETURN_TD_PERCENTAGE -> "kickReturnTdPercentage"

                // Game Flow
                Stats.NUMBER_OF_DRIVES -> "numberOfDrives"
                Stats.TIME_OF_POSSESSION -> "timeOfPossession"

                // Quarter Scores
                Stats.Q1_SCORE -> "q1Score"
                Stats.Q2_SCORE -> "q2Score"
                Stats.Q3_SCORE -> "q3Score"
                Stats.Q4_SCORE -> "q4Score"
                Stats.OT_SCORE -> "otScore"

                // Touchdowns
                Stats.TOUCHDOWNS -> "touchdowns"

                // Down Conversions
                Stats.THIRD_DOWN_CONVERSION_SUCCESS -> "thirdDownConversionSuccess"
                Stats.THIRD_DOWN_CONVERSION_ATTEMPTS -> "thirdDownConversionAttempts"
                Stats.THIRD_DOWN_CONVERSION_PERCENTAGE -> "thirdDownConversionPercentage"
                Stats.FOURTH_DOWN_CONVERSION_SUCCESS -> "fourthDownConversionSuccess"
                Stats.FOURTH_DOWN_CONVERSION_ATTEMPTS -> "fourthDownConversionAttempts"
                Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE -> "fourthDownConversionPercentage"

                // Game Control
                Stats.LARGEST_LEAD -> "largestLead"
                Stats.LARGEST_DEFICIT -> "largestDeficit"

                // Red Zone
                Stats.RED_ZONE_ATTEMPTS -> "redZoneAttempts"
                Stats.RED_ZONE_SUCCESSES -> "redZoneSuccesses"
                Stats.RED_ZONE_SUCCESS_PERCENTAGE -> "redZoneSuccessPercentage"
                Stats.RED_ZONE_PERCENTAGE -> "redZonePercentage"

                // Special Teams
                Stats.SAFETIES_FORCED -> "safetiesForced"
                Stats.SAFETIES_COMMITTED -> "safetiesCommitted"

                // Performance Metrics
                Stats.AVERAGE_OFFENSIVE_DIFF -> "averageOffensiveDiff"
                Stats.AVERAGE_DEFENSIVE_DIFF -> "averageDefensiveDiff"
                Stats.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF -> "averageOffensiveSpecialTeamsDiff"
                Stats.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF -> "averageDefensiveSpecialTeamsDiff"
                Stats.AVERAGE_DIFF -> "averageDiff"
                Stats.AVERAGE_RESPONSE_SPEED -> "averageResponseSpeed"
            }

        return try {
            val property = GameStats::class.memberProperties.find { it.name == propertyName } as? KProperty1<GameStats, Any>
            when (val value = property?.get(gameStats)) {
                is Number -> value.toDouble()
                is String -> {
                    if (value == "null" || value.isEmpty()) {
                        0.0
                    } else {
                        value.toDoubleOrNull() ?: 0.0
                    }
                }
                null -> 0.0
                else -> 0.0
            }
        } catch (e: Exception) {
            Logger.error("Error getting stat value for $statName: ${e.message}")
            0.0
        }
    }

    fun calculateAverageForStat(
        statName: Stats,
        gameStatsList: List<GameStats>,
    ): Double {
        val values = gameStatsList.mapNotNull { getStatValue(statName, it) }
        return if (values.isNotEmpty()) {
            values.average()
        } else {
            0.0
        }
    }

    /**
     * Get all available seasons (10 and above, since data unavailable for seasons 1-9)
     */
    fun getAvailableSeasons(): List<Int> {
        val allGameStats = gameStatsRepository.findAll().toList()
        val seasons = allGameStats.mapNotNull { it.season }.distinct().sorted()
        return seasons.filter { it >= 10 } // Only seasons 10 and above
    }

    /**
     * Check if a game fully completed (Q4 clock expired or went to OT)
     */
    fun isCompleteGame(game: Game): Boolean {
        return game.gameStatus == GameStatus.FINAL &&
            (game.quarter > 4 || (game.quarter == 4 && game.clock == "0:00"))
    }

    fun getCompletedGameIds(): Set<Int> {
        return gameRepository.findAll()
            .filter { it.gameStatus == GameStatus.FINAL && (it.quarter > 4 || (it.quarter == 4 && it.clock == "0:00")) }
            .map { it.gameId }
            .toSet()
    }
}
