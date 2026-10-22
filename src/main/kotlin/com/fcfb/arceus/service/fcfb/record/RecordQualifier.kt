package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.GameStats
import org.springframework.stereotype.Service
import kotlin.math.ceil
import kotlin.math.max

/**
 * Percentage and per-play records are meaningless on a tiny sample, so a candidate only qualifies once it has
 * enough of the attempts that stat is a rate of. Minimums are hand set from the season 11 averages, the one
 * complete season, and are expressed per game played so a partial or in progress season is judged on its own
 * length rather than disappearing from the records board until week 12.
 */
@Service
class RecordQualifier(
    private val recordStatUtils: RecordStatUtils,
) {
    companion object {
        const val MIN_SINGLE_GAME_PLAYS = 25
    }

    private data class SeasonMinimum(val perGame: Double, val floor: Int)

    private val genericMinimum = SeasonMinimum(perGame = 25.0, floor = 25)

    private val denominatorByStat =
        mapOf(
            Stats.PASS_COMPLETION_PERCENTAGE to Stats.PASS_ATTEMPTS,
            Stats.PASS_SUCCESS_PERCENTAGE to Stats.PASS_ATTEMPTS,
            Stats.RUSH_SUCCESS_PERCENTAGE to Stats.RUSH_ATTEMPTS,
            Stats.THIRD_DOWN_CONVERSION_PERCENTAGE to Stats.THIRD_DOWN_CONVERSION_ATTEMPTS,
            Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE to Stats.FOURTH_DOWN_CONVERSION_ATTEMPTS,
            Stats.RED_ZONE_SUCCESS_PERCENTAGE to Stats.RED_ZONE_ATTEMPTS,
            Stats.RED_ZONE_PERCENTAGE to Stats.NUMBER_OF_DRIVES,
            Stats.FIELD_GOAL_PERCENTAGE to Stats.FIELD_GOAL_ATTEMPTS,
            Stats.ONSIDE_SUCCESS_PERCENTAGE to Stats.ONSIDE_ATTEMPTS,
            Stats.TOUCHBACK_PERCENTAGE to Stats.NORMAL_KICKOFF_ATTEMPTS,
            Stats.KICK_RETURN_TD_PERCENTAGE to Stats.NUMBER_OF_KICKOFFS,
            Stats.AVERAGE_PUNT_LENGTH to Stats.PUNTS_ATTEMPTED,
            Stats.AVERAGE_PASS_YARDS_PER_ATTEMPT to Stats.PASS_ATTEMPTS,
            Stats.AVERAGE_RUSH_YARDS_PER_ATTEMPT to Stats.RUSH_ATTEMPTS,
        )

    /**
     * A rate over one game or a two game postseason says nothing a counting stat does not say better, so these
     * stats only hold a full regular season record. Per play averages stay available everywhere.
     */
    private val fullSeasonOnlyStats =
        setOf(
            Stats.PASS_COMPLETION_PERCENTAGE,
            Stats.PASS_SUCCESS_PERCENTAGE,
            Stats.RUSH_SUCCESS_PERCENTAGE,
            Stats.THIRD_DOWN_CONVERSION_PERCENTAGE,
            Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE,
            Stats.THIRD_DOWN_CONVERSION_PERCENTAGE_AGAINST,
            Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE_AGAINST,
            Stats.RED_ZONE_SUCCESS_PERCENTAGE,
            Stats.RED_ZONE_SUCCESS_PERCENTAGE_AGAINST,
            Stats.RED_ZONE_PERCENTAGE,
            Stats.FIELD_GOAL_PERCENTAGE,
            Stats.ONSIDE_SUCCESS_PERCENTAGE,
            Stats.TOUCHBACK_PERCENTAGE,
            Stats.KICK_RETURN_TD_PERCENTAGE,
            Stats.PUNT_RETURN_TD_PERCENTAGE,
            Stats.AVERAGE_PUNT_LENGTH,
            Stats.AVERAGE_PASS_YARDS_PER_ATTEMPT,
            Stats.AVERAGE_RUSH_YARDS_PER_ATTEMPT,
        )

    fun allowsRecordType(
        statName: Stats,
        recordType: RecordType,
    ): Boolean =
        !fullSeasonOnlyStats.contains(statName) ||
            recordType == RecordType.SINGLE_SEASON ||
            recordType == RecordType.SINGLE_SEASON_LOWEST

    private val minimumByDenominator =
        mapOf(
            Stats.PASS_ATTEMPTS to SeasonMinimum(perGame = 10.0, floor = 10),
            Stats.RUSH_ATTEMPTS to SeasonMinimum(perGame = 10.0, floor = 10),
            Stats.THIRD_DOWN_CONVERSION_ATTEMPTS to SeasonMinimum(perGame = 5.0, floor = 5),
            Stats.FOURTH_DOWN_CONVERSION_ATTEMPTS to SeasonMinimum(perGame = 1.0, floor = 10),
            Stats.RED_ZONE_ATTEMPTS to SeasonMinimum(perGame = 1.2, floor = 5),
            Stats.NUMBER_OF_DRIVES to SeasonMinimum(perGame = 3.0, floor = 3),
            Stats.FIELD_GOAL_ATTEMPTS to SeasonMinimum(perGame = 0.6, floor = 4),
            Stats.ONSIDE_ATTEMPTS to SeasonMinimum(perGame = 0.15, floor = 3),
            Stats.NORMAL_KICKOFF_ATTEMPTS to SeasonMinimum(perGame = 1.5, floor = 6),
            Stats.NUMBER_OF_KICKOFFS to SeasonMinimum(perGame = 1.5, floor = 6),
            Stats.PUNTS_ATTEMPTED to SeasonMinimum(perGame = 1.0, floor = 4),
        )

    private val perPlayStats =
        setOf(
            Stats.AVERAGE_YARDS_PER_PLAY,
            Stats.AVERAGE_DIFF,
            Stats.AVERAGE_OFFENSIVE_DIFF,
            Stats.AVERAGE_DEFENSIVE_DIFF,
            Stats.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_RESPONSE_SPEED,
            Stats.PUNT_RETURN_TD_PERCENTAGE,
        )

    fun isVolumeSensitive(statName: Stats): Boolean = denominatorByStat.containsKey(statName) || perPlayStats.contains(statName)

    fun qualifiesForGameRecord(
        statName: Stats,
        gameStats: GameStats,
    ): Boolean {
        if (!isVolumeSensitive(statName)) return true
        return gameStats.offensivePlayCount >= MIN_SINGLE_GAME_PLAYS
    }

    /**
     * A rate can still fall before the season ends, so an in progress season cannot hold one: the board would
     * show a percentage the team no longer has. Counting stats only climb and are checked live as usual.
     */
    fun qualifiesForSeasonRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        seasonComplete: Boolean,
    ): Boolean {
        if (!isVolumeSensitive(statName)) return true
        if (!seasonComplete) return false
        if (gameStatsList.isEmpty()) return false

        val denominator = denominatorByStat[statName]
        val total =
            if (denominator == null) {
                gameStatsList.sumOf { it.offensivePlayCount }.toDouble()
            } else {
                gameStatsList.sumOf { recordStatUtils.getStatValue(denominator, it) }
            }
        val minimum = minimumByDenominator[denominator] ?: genericMinimum
        return total >= max(minimum.floor, ceil(minimum.perGame * gameStatsList.size).toInt())
    }
}
