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

@Service
class RecordStatUtils(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
) {
    companion object {
        const val EARLIEST_VALID_SEASON = 10
    }

    val lowestOnlyStats =
        setOf(
            Stats.AVERAGE_OFFENSIVE_DIFF,
            Stats.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_RESPONSE_SPEED,
        )

    val againstBaseStat =
        mapOf(
            Stats.POINTS_AGAINST to Stats.SCORE,
            Stats.TOTAL_YARDS_AGAINST to Stats.TOTAL_YARDS,
            Stats.PASS_YARDS_AGAINST to Stats.PASS_YARDS,
            Stats.RUSH_YARDS_AGAINST to Stats.RUSH_YARDS,
            Stats.FIRST_DOWNS_AGAINST to Stats.FIRST_DOWNS,
            Stats.TOUCHDOWNS_AGAINST to Stats.TOUCHDOWNS,
            Stats.THIRD_DOWN_CONVERSION_PERCENTAGE_AGAINST to Stats.THIRD_DOWN_CONVERSION_PERCENTAGE,
            Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE_AGAINST to Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE,
            Stats.RED_ZONE_SUCCESS_PERCENTAGE_AGAINST to Stats.RED_ZONE_SUCCESS_PERCENTAGE,
        )

    val percentageStats =
        setOf(
            Stats.PASS_COMPLETION_PERCENTAGE,
            Stats.PASS_SUCCESS_PERCENTAGE,
            Stats.RUSH_SUCCESS_PERCENTAGE,
            Stats.FIELD_GOAL_PERCENTAGE,
            Stats.PUNT_RETURN_TD_PERCENTAGE,
            Stats.ONSIDE_SUCCESS_PERCENTAGE,
            Stats.TOUCHBACK_PERCENTAGE,
            Stats.KICK_RETURN_TD_PERCENTAGE,
            Stats.THIRD_DOWN_CONVERSION_PERCENTAGE,
            Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE,
            Stats.RED_ZONE_SUCCESS_PERCENTAGE,
            Stats.RED_ZONE_PERCENTAGE,
        )

    val averagedStats =
        setOf(
            Stats.PASS_COMPLETION_PERCENTAGE,
            Stats.PASS_SUCCESS_PERCENTAGE,
            Stats.RUSH_SUCCESS_PERCENTAGE,
            Stats.FIELD_GOAL_PERCENTAGE,
            Stats.PUNT_RETURN_TD_PERCENTAGE,
            Stats.ONSIDE_SUCCESS_PERCENTAGE,
            Stats.TOUCHBACK_PERCENTAGE,
            Stats.KICK_RETURN_TD_PERCENTAGE,
            Stats.THIRD_DOWN_CONVERSION_PERCENTAGE,
            Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE,
            Stats.RED_ZONE_SUCCESS_PERCENTAGE,
            Stats.RED_ZONE_PERCENTAGE,
            Stats.AVERAGE_PUNT_LENGTH,
        )

    val dualRecordStats =
        setOf(
            Stats.AVERAGE_DEFENSIVE_DIFF,
            Stats.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_DIFF,
            Stats.TIME_OF_POSSESSION,
            Stats.TOTAL_YARDS,
            Stats.AVERAGE_YARDS_PER_PLAY,
            Stats.FIRST_DOWNS,
            Stats.PASS_ATTEMPTS,
            Stats.PASS_COMPLETIONS,
            Stats.PASS_YARDS,
            Stats.PASS_TOUCHDOWNS,
            Stats.PASS_SUCCESSES,
            Stats.RUSH_ATTEMPTS,
            Stats.RUSH_SUCCESSES,
            Stats.RUSH_YARDS,
            Stats.RUSH_TOUCHDOWNS,
            Stats.THIRD_DOWN_CONVERSION_SUCCESS,
            Stats.THIRD_DOWN_CONVERSION_ATTEMPTS,
            Stats.FOURTH_DOWN_CONVERSION_SUCCESS,
            Stats.FOURTH_DOWN_CONVERSION_ATTEMPTS,
            Stats.RED_ZONE_ATTEMPTS,
            Stats.RED_ZONE_SUCCESSES,
            Stats.TOUCHDOWNS,
            Stats.NUMBER_OF_KICKOFFS,
            Stats.ONSIDE_ATTEMPTS,
            Stats.ONSIDE_SUCCESS,
            Stats.NORMAL_KICKOFF_ATTEMPTS,
            Stats.TOUCHBACKS,
            Stats.KICK_RETURN_TD,
        )

    val generalRecordStats =
        setOf(
            Stats.LONGEST_PASS,
            Stats.LONGEST_RUN,
            Stats.LONGEST_FIELD_GOAL,
            Stats.LONGEST_PUNT,
        )

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

    fun getStatValue(
        statName: Stats,
        gameStats: GameStats,
    ): Double {
        val propertyName =
            when (statName) {
                Stats.SCORE -> "score"

                Stats.PASS_ATTEMPTS -> "passAttempts"
                Stats.PASS_COMPLETIONS -> "passCompletions"
                Stats.PASS_COMPLETION_PERCENTAGE -> "passCompletionPercentage"
                Stats.PASS_YARDS -> "passYards"
                Stats.LONGEST_PASS -> "longestPass"
                Stats.PASS_TOUCHDOWNS -> "passTouchdowns"
                Stats.PASS_SUCCESSES -> "passSuccesses"
                Stats.PASS_SUCCESS_PERCENTAGE -> "passSuccessPercentage"

                Stats.RUSH_ATTEMPTS -> "rushAttempts"
                Stats.RUSH_SUCCESSES -> "rushSuccesses"
                Stats.RUSH_SUCCESS_PERCENTAGE -> "rushSuccessPercentage"
                Stats.RUSH_YARDS -> "rushYards"
                Stats.LONGEST_RUN -> "longestRun"
                Stats.RUSH_TOUCHDOWNS -> "rushTouchdowns"

                Stats.TOTAL_YARDS -> "totalYards"
                Stats.AVERAGE_YARDS_PER_PLAY -> "averageYardsPerPlay"
                Stats.FIRST_DOWNS -> "firstDowns"

                Stats.SACKS_ALLOWED -> "sacksAllowed"
                Stats.SACKS_FORCED -> "sacksForced"

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

                Stats.FIELD_GOAL_MADE -> "fieldGoalMade"
                Stats.FIELD_GOAL_ATTEMPTS -> "fieldGoalAttempts"
                Stats.FIELD_GOAL_PERCENTAGE -> "fieldGoalPercentage"
                Stats.LONGEST_FIELD_GOAL -> "longestFieldGoal"
                Stats.BLOCKED_OPPONENT_FIELD_GOALS -> "blockedOpponentFieldGoals"
                Stats.FIELD_GOAL_TOUCHDOWN -> "fieldGoalTouchdown"

                Stats.PUNTS_ATTEMPTED -> "puntsAttempted"
                Stats.LONGEST_PUNT -> "longestPunt"
                Stats.AVERAGE_PUNT_LENGTH -> "averagePuntLength"
                Stats.BLOCKED_OPPONENT_PUNT -> "blockedOpponentPunt"
                Stats.PUNT_RETURN_TD -> "puntReturnTd"
                Stats.PUNT_RETURN_TD_PERCENTAGE -> "puntReturnTdPercentage"

                Stats.NUMBER_OF_KICKOFFS -> "numberOfKickoffs"
                Stats.ONSIDE_ATTEMPTS -> "onsideAttempts"
                Stats.ONSIDE_SUCCESS -> "onsideSuccess"
                Stats.ONSIDE_SUCCESS_PERCENTAGE -> "onsideSuccessPercentage"
                Stats.NORMAL_KICKOFF_ATTEMPTS -> "normalKickoffAttempts"
                Stats.TOUCHBACKS -> "touchbacks"
                Stats.TOUCHBACK_PERCENTAGE -> "touchbackPercentage"
                Stats.KICK_RETURN_TD -> "kickReturnTd"
                Stats.KICK_RETURN_TD_PERCENTAGE -> "kickReturnTdPercentage"

                Stats.NUMBER_OF_DRIVES -> "numberOfDrives"
                Stats.TIME_OF_POSSESSION -> "timeOfPossession"

                Stats.Q1_SCORE -> "q1Score"
                Stats.Q2_SCORE -> "q2Score"
                Stats.Q3_SCORE -> "q3Score"
                Stats.Q4_SCORE -> "q4Score"
                Stats.OT_SCORE -> "otScore"

                Stats.TOUCHDOWNS -> "touchdowns"

                Stats.THIRD_DOWN_CONVERSION_SUCCESS -> "thirdDownConversionSuccess"
                Stats.THIRD_DOWN_CONVERSION_ATTEMPTS -> "thirdDownConversionAttempts"
                Stats.THIRD_DOWN_CONVERSION_PERCENTAGE -> "thirdDownConversionPercentage"
                Stats.FOURTH_DOWN_CONVERSION_SUCCESS -> "fourthDownConversionSuccess"
                Stats.FOURTH_DOWN_CONVERSION_ATTEMPTS -> "fourthDownConversionAttempts"
                Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE -> "fourthDownConversionPercentage"

                Stats.LARGEST_LEAD -> "largestLead"
                Stats.LARGEST_DEFICIT -> "largestDeficit"

                Stats.RED_ZONE_ATTEMPTS -> "redZoneAttempts"
                Stats.RED_ZONE_SUCCESSES -> "redZoneSuccesses"
                Stats.RED_ZONE_SUCCESS_PERCENTAGE -> "redZoneSuccessPercentage"
                Stats.RED_ZONE_PERCENTAGE -> "redZonePercentage"

                Stats.SAFETIES_FORCED -> "safetiesForced"
                Stats.SAFETIES_COMMITTED -> "safetiesCommitted"

                Stats.AVERAGE_OFFENSIVE_DIFF -> "averageOffensiveDiff"
                Stats.AVERAGE_DEFENSIVE_DIFF -> "averageDefensiveDiff"
                Stats.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF -> "averageOffensiveSpecialTeamsDiff"
                Stats.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF -> "averageDefensiveSpecialTeamsDiff"
                Stats.AVERAGE_DIFF -> "averageDiff"
                Stats.AVERAGE_RESPONSE_SPEED -> "averageResponseSpeed"

                Stats.POINTS_AGAINST -> "score"
                Stats.TOTAL_YARDS_AGAINST -> "totalYards"
                Stats.PASS_YARDS_AGAINST -> "passYards"
                Stats.RUSH_YARDS_AGAINST -> "rushYards"
                Stats.FIRST_DOWNS_AGAINST -> "firstDowns"
                Stats.TOUCHDOWNS_AGAINST -> "touchdowns"
                Stats.THIRD_DOWN_CONVERSION_PERCENTAGE_AGAINST -> "thirdDownConversionPercentage"
                Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE_AGAINST -> "fourthDownConversionPercentage"
                Stats.RED_ZONE_SUCCESS_PERCENTAGE_AGAINST -> "redZoneSuccessPercentage"
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

    private val rateComponents =
        mapOf(
            Stats.PASS_COMPLETION_PERCENTAGE to (Stats.PASS_COMPLETIONS to Stats.PASS_ATTEMPTS),
            Stats.PASS_SUCCESS_PERCENTAGE to (Stats.PASS_SUCCESSES to Stats.PASS_ATTEMPTS),
            Stats.RUSH_SUCCESS_PERCENTAGE to (Stats.RUSH_SUCCESSES to Stats.RUSH_ATTEMPTS),
            Stats.FIELD_GOAL_PERCENTAGE to (Stats.FIELD_GOAL_MADE to Stats.FIELD_GOAL_ATTEMPTS),
            Stats.THIRD_DOWN_CONVERSION_PERCENTAGE to (Stats.THIRD_DOWN_CONVERSION_SUCCESS to Stats.THIRD_DOWN_CONVERSION_ATTEMPTS),
            Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE to (Stats.FOURTH_DOWN_CONVERSION_SUCCESS to Stats.FOURTH_DOWN_CONVERSION_ATTEMPTS),
            Stats.RED_ZONE_SUCCESS_PERCENTAGE to (Stats.RED_ZONE_SUCCESSES to Stats.RED_ZONE_ATTEMPTS),
            Stats.RED_ZONE_PERCENTAGE to (Stats.RED_ZONE_ATTEMPTS to Stats.NUMBER_OF_DRIVES),
            Stats.ONSIDE_SUCCESS_PERCENTAGE to (Stats.ONSIDE_SUCCESS to Stats.ONSIDE_ATTEMPTS),
            Stats.TOUCHBACK_PERCENTAGE to (Stats.TOUCHBACKS to Stats.NORMAL_KICKOFF_ATTEMPTS),
        )

    private val seasonMeanStats =
        setOf(
            Stats.AVERAGE_DIFF,
            Stats.AVERAGE_OFFENSIVE_DIFF,
            Stats.AVERAGE_DEFENSIVE_DIFF,
            Stats.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_YARDS_PER_PLAY,
            Stats.AVERAGE_RESPONSE_SPEED,
            Stats.KICK_RETURN_TD_PERCENTAGE,
            Stats.PUNT_RETURN_TD_PERCENTAGE,
        )

    fun calculateSeasonValue(
        statName: Stats,
        gameStatsList: List<GameStats>,
    ): Double {
        if (gameStatsList.isEmpty()) return 0.0
        rateComponents[statName]?.let { (numerator, denominator) ->
            val total = gameStatsList.sumOf { getStatValue(numerator, it) }
            val attempts = gameStatsList.sumOf { getStatValue(denominator, it) }
            return if (attempts > 0) total / attempts * 100 else 0.0
        }
        if (statName == Stats.AVERAGE_PUNT_LENGTH) {
            val totalPuntYards =
                gameStatsList.sumOf {
                    getStatValue(Stats.AVERAGE_PUNT_LENGTH, it) * getStatValue(Stats.PUNTS_ATTEMPTED, it)
                }
            val punts = gameStatsList.sumOf { getStatValue(Stats.PUNTS_ATTEMPTED, it) }
            return if (punts > 0) totalPuntYards / punts else 0.0
        }
        return if (seasonMeanStats.contains(statName)) {
            gameStatsList.map { getStatValue(statName, it) }.average()
        } else {
            gameStatsList.sumOf { getStatValue(statName, it) }
        }
    }

    fun getAvailableSeasons(): List<Int> {
        val allGameStats = gameStatsRepository.findAll().toList()
        val seasons = allGameStats.mapNotNull { it.season }.distinct().sorted()
        return seasons.filter { it >= EARLIEST_VALID_SEASON }
    }

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
