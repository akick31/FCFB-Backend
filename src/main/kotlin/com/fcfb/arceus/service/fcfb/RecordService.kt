package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.repositories.SeasonRepository
import com.fcfb.arceus.service.specification.RecordSpecificationService
import com.fcfb.arceus.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Service
class RecordService(
    private val recordRepository: RecordRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
    private val recordSpecificationService: RecordSpecificationService,
    private val seasonRepository: SeasonRepository,
) {
    /**
     * Get filtered records with pagination
     */
    fun getFilteredRecords(
        season: Int?,
        recordType: RecordType?,
        recordName: Stats?,
        pageable: Pageable,
    ): Page<Record> {
        val spec = recordSpecificationService.createSpecification(season, recordType, recordName)
        val sortOrders = recordSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return recordRepository.findAll(spec, sortedPageable)
    }

    /**
     * Stats that should track both highest and lowest values
     */
    private val dualRecordStats =
        setOf(
            Stats.AVERAGE_OFFENSIVE_DIFF,
            Stats.AVERAGE_DEFENSIVE_DIFF,
            Stats.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF,
            Stats.AVERAGE_DIFF,
            Stats.AVERAGE_RESPONSE_SPEED,
            Stats.TIME_OF_POSSESSION,
            Stats.TOTAL_YARDS,
            Stats.AVERAGE_YARDS_PER_PLAY,
        )

    /**
     * Stats that are general records (don't need season/game distinction)
     * These are things like "longest field goal ever" or "fastest response time ever"
     */
    private val generalRecordStats =
        setOf(
            Stats.LONGEST_PASS,
            Stats.LONGEST_RUN,
            Stats.LONGEST_FIELD_GOAL,
            Stats.LONGEST_PUNT,
        )

    /**
     * Stats that are percentages and should be calculated from season totals
     * instead of summing individual game percentages
     */
    private val percentageStats =
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

    /**
     * Stats that are GAME-specific only (not applicable to season records)
     * These are things like time of possession, number of drives, quarter scores, score
     */
    private val gameOnlyStats =
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
     * Generate all records for all seasons
     */
    fun generateAllRecords() {
        Logger.info("Starting generation of all records")

        // Clear existing records
        recordRepository.deleteAll()

        // Get all available seasons (10 and above, since data unavailable for seasons 1-9)
        val availableSeasons = getAvailableSeasons()

        for (season in availableSeasons) {
            generateRecordsForSeason(season)
        }

        Logger.info("Completed generation of all records")
    }

    /**
     * Check if a game broke any records and update them
     */
    fun checkAndUpdateRecordsForGame(game: Game) {
        Logger.info("Checking records for game ${game.gameId}")

        val homeStats = gameStatsRepository.getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
        val awayStats = gameStatsRepository.getGameStatsByIdAndTeam(game.gameId, game.awayTeam)

        val gameStatsList = listOfNotNull(homeStats, awayStats)

        // Get current season stats for both teams (for season record checking)
        val currentSeason = game.season ?: return
        val homeSeasonStats = getCurrentSeasonStatsForTeam(game.homeTeam, currentSeason)
        val awaySeasonStats = getCurrentSeasonStatsForTeam(game.awayTeam, currentSeason)
        val currentSeasonStats = listOfNotNull(homeSeasonStats, awaySeasonStats)

        // Check each stat type
        for (stat in Stats.values()) {
            if (generalRecordStats.contains(stat)) {
                // General records - just check highest/lowest ever
                if (dualRecordStats.contains(stat)) {
                    checkAndUpdateGeneralRecord(stat, gameStatsList, game, RecordType.GENERAL)
                    checkAndUpdateGeneralRecord(stat, gameStatsList, game, RecordType.GENERAL_LOWEST)
                } else {
                    checkAndUpdateGeneralRecord(stat, gameStatsList, game, RecordType.GENERAL)
                }
            } else if (gameOnlyStats.contains(stat)) {
                // Game-only records - only check game records
                if (dualRecordStats.contains(stat)) {
                    checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME)
                    checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME_LOWEST)
                } else {
                    checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME)
                }
            } else {
                // Regular stats - check both game and season records
                if (dualRecordStats.contains(stat)) {
                    // Check both highest and lowest records for dual-record stats
                    checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME)
                    checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME_LOWEST)
                    checkAndUpdateSeasonRecord(stat, homeSeasonStats, awaySeasonStats, RecordType.SINGLE_SEASON)
                    checkAndUpdateSeasonRecord(stat, homeSeasonStats, awaySeasonStats, RecordType.SINGLE_SEASON_LOWEST)
                } else {
                    // Check only highest records for regular stats
                    checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME)
                    checkAndUpdateSeasonRecord(stat, homeSeasonStats, awaySeasonStats, RecordType.SINGLE_SEASON)
                }
            }
        }

        Logger.info("Completed checking records for game ${game.gameId}")
    }

    /**
     * Generate records for a specific season
     */
    private fun generateRecordsForSeason(seasonNumber: Int) {
        Logger.info("Generating records for season $seasonNumber")

        val seasonGameStats = gameStatsRepository.findAll().filter { it.season == seasonNumber }

        for (stat in Stats.values()) {
            if (generalRecordStats.contains(stat)) {
                // General records - just track highest/lowest ever
                if (dualRecordStats.contains(stat)) {
                    generateGeneralRecord(stat, seasonGameStats, RecordType.GENERAL)
                    generateGeneralRecord(stat, seasonGameStats, RecordType.GENERAL_LOWEST)
                } else {
                    generateGeneralRecord(stat, seasonGameStats, RecordType.GENERAL)
                }
            } else if (gameOnlyStats.contains(stat)) {
                // Game-only records - only track game records
                if (dualRecordStats.contains(stat)) {
                    generateGameRecord(stat, seasonGameStats, RecordType.SINGLE_GAME)
                    generateGameRecord(stat, seasonGameStats, RecordType.SINGLE_GAME_LOWEST)
                } else {
                    generateGameRecord(stat, seasonGameStats, RecordType.SINGLE_GAME)
                }
            } else {
                // Regular stats - track both game and season records
                if (dualRecordStats.contains(stat)) {
                    // Generate both highest and lowest records for dual-record stats
                    generateGameRecord(stat, seasonGameStats, RecordType.SINGLE_GAME)
                    generateGameRecord(stat, seasonGameStats, RecordType.SINGLE_GAME_LOWEST)
                    generateSeasonRecord(stat, seasonGameStats, RecordType.SINGLE_SEASON)
                    generateSeasonRecord(stat, seasonGameStats, RecordType.SINGLE_SEASON_LOWEST)
                } else {
                    // Generate only highest records for regular stats
                    generateGameRecord(stat, seasonGameStats, RecordType.SINGLE_GAME)
                    generateSeasonRecord(stat, seasonGameStats, RecordType.SINGLE_SEASON)
                }
            }
        }
    }

    /**
     * Generate game records (single game performance)
     */
    private fun generateGameRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
    ) {
        val isLowest = recordType == RecordType.SINGLE_GAME_LOWEST
        val bestGameStats =
            if (isLowest) {
                gameStatsList.minByOrNull { getStatValue(statName, it) }
            } else {
                gameStatsList.maxByOrNull { getStatValue(statName, it) }
            } ?: return

        val statValue = getStatValue(statName, bestGameStats)

        val record =
            Record(
                recordName = statName,
                recordType = recordType,
                seasonNumber = bestGameStats.season ?: 0,
                week = bestGameStats.week ?: 0,
                gameId = bestGameStats.gameId,
                homeTeam = getHomeTeamForGame(bestGameStats.gameId),
                awayTeam = getAwayTeamForGame(bestGameStats.gameId),
                recordTeam = bestGameStats.team ?: "",
                coach = getCoachForGameRecord(bestGameStats),
                recordValue = statValue,
            )

        recordRepository.save(record)
    }

    /**
     * Generate season records (best season performance ever)
     */
    private fun generateSeasonRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
    ) {
        // Get game stats only for available seasons (10 and above, data unavailable for seasons 1-9)
        val availableSeasons = getAvailableSeasons()

        // For percentage stats, only include seasons that have ended (have end_date)
        val seasonsToProcess =
            if (percentageStats.contains(statName)) {
                val finishedSeasons =
                    seasonRepository.findAll()
                        .filter { it.endDate != null }
                        .map { it.seasonNumber }
                        .filter { it in availableSeasons }
                finishedSeasons
            } else {
                availableSeasons
            }

        val allGameStats =
            gameStatsRepository.findAll().toList()
                .filter { it.season in seasonsToProcess }

        // Group by team and season, then calculate season totals/averages
        val teamSeasonTotals =
            allGameStats
                .groupBy { "${it.team}_${it.season}" }
                .mapValues { (_, stats) ->
                    when {
                        percentageStats.contains(statName) -> {
                            // For percentage stats, calculate from season totals
                            calculatePercentageForStat(statName, stats)
                        }
                        dualRecordStats.contains(statName) -> {
                            // For diff stats, calculate average instead of sum
                            calculateAverageForStat(statName, stats)
                        }
                        else -> {
                            // For regular stats, sum the values
                            stats.sumOf { getStatValue(statName, it) }
                        }
                    }
                }

        val isLowest = recordType == RecordType.SINGLE_SEASON_LOWEST
        val bestTeamSeason =
            if (isLowest) {
                teamSeasonTotals.minByOrNull { it.value }
            } else {
                teamSeasonTotals.maxByOrNull { it.value }
            } ?: return

        val (team, seasonStr) = bestTeamSeason.key.split("_")
        val season = seasonStr.toInt()
        val teamSeasonGameStats = allGameStats.filter { it.team == team && it.season == season }
        val bestGameStats = teamSeasonGameStats.first()

        val record =
            Record(
                recordName = statName,
                recordType = recordType,
                seasonNumber = season,
                // Season records don't have a specific week
                week = null,
                // Season records don't have a specific game ID
                gameId = null,
                // Season records don't have specific home/away teams
                homeTeam = null,
                // Season records don't have specific home/away teams
                awayTeam = null,
                recordTeam = team,
                coach = getCoachForSeasonRecord(teamSeasonGameStats),
                recordValue = bestTeamSeason.value,
            )

        recordRepository.save(record)
    }

    /**
     * Generate general records (all-time records that don't need season/game distinction)
     */
    private fun generateGeneralRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
    ) {
        // Get game stats only for seasons 10 and 11 (data unavailable for seasons 1-9)
        val allGameStats =
            gameStatsRepository.findAll().toList()
                .filter { it.season == 10 || it.season == 11 }

        val isLowest = recordType == RecordType.GENERAL_LOWEST
        val bestGameStats =
            if (isLowest) {
                allGameStats.minByOrNull { getStatValue(statName, it) }
            } else {
                allGameStats.maxByOrNull { getStatValue(statName, it) }
            } ?: return

        val record =
            Record(
                recordName = statName,
                recordType = recordType,
                seasonNumber = bestGameStats.season ?: 0,
                week = bestGameStats.week ?: 0,
                gameId = bestGameStats.gameId,
                homeTeam = getHomeTeamForGame(bestGameStats.gameId),
                awayTeam = getAwayTeamForGame(bestGameStats.gameId),
                recordTeam = bestGameStats.team ?: "",
                coach = getCoachForGameRecord(bestGameStats),
                recordValue = getStatValue(statName, bestGameStats),
            )

        recordRepository.save(record)
    }

    /**
     * Check and update game record for a specific stat
     */
    private fun checkAndUpdateGameRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        game: Game,
        recordType: RecordType,
    ) {
        val currentRecord = recordRepository.findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(statName, recordType)

        for (gameStats in gameStatsList) {
            val currentValue = getStatValue(statName, gameStats)
            val recordValue = currentRecord?.recordValue ?: if (recordType == RecordType.SINGLE_GAME_LOWEST) Double.MAX_VALUE else 0.0

            val isNewRecord =
                if (recordType == RecordType.SINGLE_GAME_LOWEST) {
                    currentValue < recordValue
                } else {
                    currentValue > recordValue
                }

            if (isNewRecord) {
                // New record!
                val newRecord =
                    Record(
                        recordName = statName,
                        recordType = recordType,
                        seasonNumber = game.season ?: 0,
                        week = game.week ?: 0,
                        gameId = game.gameId,
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        recordTeam = gameStats.team ?: "",
                        coach = getCoachForGameRecord(gameStats),
                        recordValue = currentValue,
                        previousRecordValue = recordValue,
                        previousRecordTeam = currentRecord?.recordTeam,
                        previousRecordGameId = currentRecord?.gameId,
                    )

                recordRepository.save(newRecord)
                val recordTypeStr = if (recordType == RecordType.SINGLE_GAME_LOWEST) "LOWEST SINGLE GAME" else "SINGLE GAME"
                Logger.info("New $recordTypeStr record: ${statName.name} = $currentValue by ${gameStats.team} in game ${game.gameId}")
            }
        }
    }

    /**
     * Get current season stats for a team
     */
    private fun getCurrentSeasonStatsForTeam(
        team: String,
        season: Int,
    ): GameStats? {
        return gameStatsRepository.findAll()
            .filter { it.team == team && it.season == season }
            .firstOrNull()
    }

    /**
     * Check and update season record for a specific stat
     */
    private fun checkAndUpdateSeasonRecord(
        statName: Stats,
        homeSeasonStats: GameStats?,
        awaySeasonStats: GameStats?,
        recordType: RecordType,
    ) {
        // Skip percentage stats - they should only be calculated when the season ends
        if (percentageStats.contains(statName)) {
            return
        }

        val currentRecord = recordRepository.findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(statName, recordType)

        // Check both teams' season stats
        val teamStats = listOfNotNull(homeSeasonStats, awaySeasonStats)

        val isLowest = recordType == RecordType.SINGLE_SEASON_LOWEST
        val bestTeamStats =
            if (isLowest) {
                teamStats.minByOrNull { gameStats ->
                    getStatValue(statName, gameStats)
                }
            } else {
                teamStats.maxByOrNull { gameStats ->
                    getStatValue(statName, gameStats)
                }
            }

        if (bestTeamStats != null) {
            val currentValue = getStatValue(statName, bestTeamStats)
            val recordValue = currentRecord?.recordValue ?: if (isLowest) Double.MAX_VALUE else 0.0

            val isNewRecord =
                if (isLowest) {
                    currentValue < recordValue
                } else {
                    currentValue > recordValue
                }

            if (isNewRecord) {
                // New record!
                val team = bestTeamStats.team ?: return
                val season = bestTeamStats.season ?: return

                val newRecord =
                    Record(
                        recordName = statName,
                        recordType = recordType,
                        seasonNumber = season,
                        // Season records don't have a specific week
                        week = null,
                        // Season records don't have a specific game ID
                        gameId = null,
                        // Season records don't have specific home/away teams
                        homeTeam = null,
                        // Season records don't have specific home/away teams
                        awayTeam = null,
                        recordTeam = team,
                        coach = getCoachForSeasonRecord(listOf(bestTeamStats)),
                        recordValue = currentValue,
                        previousRecordValue = recordValue,
                        previousRecordTeam = currentRecord?.recordTeam,
                        previousRecordGameId = currentRecord?.gameId,
                    )

                recordRepository.save(newRecord)
                val recordTypeStr = if (isLowest) "LOWEST SINGLE SEASON" else "SINGLE SEASON"
                Logger.info("New $recordTypeStr record: ${statName.name} = $currentValue by $team in season $season")
            }
        }
    }

    /**
     * Check and update general record for a specific stat
     */
    private fun checkAndUpdateGeneralRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        game: Game,
        recordType: RecordType,
    ) {
        val currentRecord = recordRepository.findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(statName, recordType)

        for (gameStats in gameStatsList) {
            val currentValue = getStatValue(statName, gameStats)
            val recordValue = currentRecord?.recordValue ?: if (recordType == RecordType.GENERAL_LOWEST) Double.MAX_VALUE else 0.0

            val isNewRecord =
                if (recordType == RecordType.GENERAL_LOWEST) {
                    currentValue < recordValue
                } else {
                    currentValue > recordValue
                }

            if (isNewRecord) {
                // New record!
                val newRecord =
                    Record(
                        recordName = statName,
                        recordType = recordType,
                        seasonNumber = game.season ?: 0,
                        week = game.week ?: 0,
                        gameId = game.gameId,
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        recordTeam = gameStats.team ?: "",
                        coach = getCoachForGameRecord(gameStats),
                        recordValue = currentValue,
                        previousRecordValue = recordValue,
                        previousRecordTeam = currentRecord?.recordTeam,
                        previousRecordGameId = currentRecord?.gameId,
                    )

                recordRepository.save(newRecord)
                val recordTypeStr = if (recordType == RecordType.GENERAL_LOWEST) "LOWEST GENERAL" else "GENERAL"
                Logger.info("New $recordTypeStr record: ${statName.name} = $currentValue by ${gameStats.team} in game ${game.gameId}")
            }
        }
    }

    /**
     * Get the value of a specific stat from GameStats using reflection
     */
    private fun getStatValue(
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

    /**
     * Get home team for a game
     */
    private fun getHomeTeamForGame(gameId: Int): String {
        return try {
            gameRepository.getGameById(gameId)?.homeTeam ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get away team for a game
     */
    private fun getAwayTeamForGame(gameId: Int): String {
        return try {
            gameRepository.getGameById(gameId)?.awayTeam ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Calculate average for diff stats across multiple games
     */
    private fun calculateAverageForStat(
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
    private fun getAvailableSeasons(): List<Int> {
        val allGameStats = gameStatsRepository.findAll().toList()
        val seasons = allGameStats.mapNotNull { it.season }.distinct().sorted()
        return seasons.filter { it >= 10 } // Only seasons 10 and above
    }

    /**
     * Get the coach for a single game record
     */
    private fun getCoachForGameRecord(gameStats: GameStats): String? {
        return gameStats.coaches?.joinToString(", ") ?: null
    }

    /**
     * Get the coach for a season record (coach who coached in the most games)
     */
    private fun getCoachForSeasonRecord(gameStatsList: List<GameStats>): String? {
        if (gameStatsList.isEmpty()) return null

        // Count how many games each coach coached
        val coachGameCounts = mutableMapOf<String, Int>()

        for (gameStats in gameStatsList) {
            gameStats.coaches?.forEach { coach ->
                coachGameCounts[coach] = coachGameCounts.getOrDefault(coach, 0) + 1
            }
        }

        // Return the coach who coached in the most games
        return coachGameCounts.maxByOrNull { it.value }?.key
    }

    /**
     * Calculate percentage for a specific stat from season totals
     */
    private fun calculatePercentageForStat(
        statName: Stats,
        gameStatsList: List<GameStats>,
    ): Double {
        return when (statName) {
            Stats.PASS_COMPLETION_PERCENTAGE -> {
                val completions = gameStatsList.sumOf { getStatValue(Stats.PASS_COMPLETIONS, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.PASS_ATTEMPTS, it) }
                if (attempts > 0) (completions / attempts) * 100.0 else 0.0
            }
            Stats.PASS_SUCCESS_PERCENTAGE -> {
                val successes = gameStatsList.sumOf { getStatValue(Stats.PASS_SUCCESSES, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.PASS_ATTEMPTS, it) }
                if (attempts > 0) (successes / attempts) * 100.0 else 0.0
            }
            Stats.RUSH_SUCCESS_PERCENTAGE -> {
                val successes = gameStatsList.sumOf { getStatValue(Stats.RUSH_SUCCESSES, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.RUSH_ATTEMPTS, it) }
                if (attempts > 0) (successes / attempts) * 100.0 else 0.0
            }
            Stats.FIELD_GOAL_PERCENTAGE -> {
                val made = gameStatsList.sumOf { getStatValue(Stats.FIELD_GOAL_MADE, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.FIELD_GOAL_ATTEMPTS, it) }
                if (attempts > 0) (made / attempts) * 100.0 else 0.0
            }
            Stats.PUNT_RETURN_TD_PERCENTAGE -> {
                val tds = gameStatsList.sumOf { getStatValue(Stats.PUNT_RETURN_TD, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.PUNTS_ATTEMPTED, it) }
                if (attempts > 0) (tds / attempts) * 100.0 else 0.0
            }
            Stats.ONSIDE_SUCCESS_PERCENTAGE -> {
                val successes = gameStatsList.sumOf { getStatValue(Stats.ONSIDE_SUCCESS, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.ONSIDE_ATTEMPTS, it) }
                if (attempts > 0) (successes / attempts) * 100.0 else 0.0
            }
            Stats.TOUCHBACK_PERCENTAGE -> {
                val touchbacks = gameStatsList.sumOf { getStatValue(Stats.TOUCHBACKS, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.NORMAL_KICKOFF_ATTEMPTS, it) }
                if (attempts > 0) (touchbacks / attempts) * 100.0 else 0.0
            }
            Stats.KICK_RETURN_TD_PERCENTAGE -> {
                val tds = gameStatsList.sumOf { getStatValue(Stats.KICK_RETURN_TD, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.NUMBER_OF_KICKOFFS, it) }
                if (attempts > 0) (tds / attempts) * 100.0 else 0.0
            }
            Stats.THIRD_DOWN_CONVERSION_PERCENTAGE -> {
                val successes = gameStatsList.sumOf { getStatValue(Stats.THIRD_DOWN_CONVERSION_SUCCESS, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.THIRD_DOWN_CONVERSION_ATTEMPTS, it) }
                if (attempts > 0) (successes / attempts) * 100.0 else 0.0
            }
            Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE -> {
                val successes = gameStatsList.sumOf { getStatValue(Stats.FOURTH_DOWN_CONVERSION_SUCCESS, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.FOURTH_DOWN_CONVERSION_ATTEMPTS, it) }
                if (attempts > 0) (successes / attempts) * 100.0 else 0.0
            }
            Stats.RED_ZONE_SUCCESS_PERCENTAGE -> {
                val successes = gameStatsList.sumOf { getStatValue(Stats.RED_ZONE_SUCCESSES, it) }
                val attempts = gameStatsList.sumOf { getStatValue(Stats.RED_ZONE_ATTEMPTS, it) }
                if (attempts > 0) (successes / attempts) * 100.0 else 0.0
            }
            Stats.RED_ZONE_PERCENTAGE -> {
                val attempts = gameStatsList.sumOf { getStatValue(Stats.RED_ZONE_ATTEMPTS, it) }
                val totalDrives = gameStatsList.sumOf { getStatValue(Stats.NUMBER_OF_DRIVES, it) }
                if (totalDrives > 0) (attempts / totalDrives) * 100.0 else 0.0
            }
            else -> 0.0
        }
    }
}
