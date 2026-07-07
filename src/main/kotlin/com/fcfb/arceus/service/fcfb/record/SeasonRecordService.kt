package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service

/**
 * Handles generation and in-flight checking of single-season records.
 */
@Service
class SeasonRecordService(
    private val recordRepository: RecordRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val recordStatUtils: RecordStatUtils,
) {
    fun generateSeasonRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
    ) {
        // Get game stats only for available seasons (10 and above, data unavailable for seasons 1-9)
        val availableSeasons = recordStatUtils.getAvailableSeasons()

        val allGameStats =
            gameStatsRepository.findAll().toList()
                .filter { it.season in availableSeasons }

        // Group by team and season, then calculate season totals/averages
        val teamSeasonTotals =
            allGameStats
                .groupBy { "${it.team}_${it.season}" }
                .mapValues { (_, stats) ->
                    when {
                        recordStatUtils.lowestOnlyStats.contains(statName) || recordStatUtils.dualRecordStats.contains(statName) -> {
                            // For diff stats, calculate average instead of sum
                            recordStatUtils.calculateAverageForStat(statName, stats)
                        }
                        else -> {
                            // For regular stats, sum the values
                            stats.sumOf { recordStatUtils.getStatValue(statName, it) }
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

    fun getCurrentSeasonStatsForTeam(
        team: String,
        season: Int,
    ): GameStats? {
        return gameStatsRepository.findAll()
            .filter { it.team == team && it.season == season }
            .firstOrNull()
    }

    fun checkAndUpdateSeasonRecord(
        statName: Stats,
        homeSeasonStats: GameStats?,
        awaySeasonStats: GameStats?,
        recordType: RecordType,
    ) {
        val currentRecord = recordRepository.findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(statName, recordType)

        // Check both teams' season stats
        val teamStats = listOfNotNull(homeSeasonStats, awaySeasonStats)

        val isLowest = recordType == RecordType.SINGLE_SEASON_LOWEST
        val bestTeamStats =
            if (isLowest) {
                teamStats.minByOrNull { gameStats ->
                    recordStatUtils.getStatValue(statName, gameStats)
                }
            } else {
                teamStats.maxByOrNull { gameStats ->
                    recordStatUtils.getStatValue(statName, gameStats)
                }
            }

        if (bestTeamStats != null) {
            val currentValue = recordStatUtils.getStatValue(statName, bestTeamStats)
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
}
