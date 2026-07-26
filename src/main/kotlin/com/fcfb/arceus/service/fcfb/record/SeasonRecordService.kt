package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordScope
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
    private data class TeamSeason(val team: String, val season: Int, val value: Double)

    fun generateSeasonRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
        teamConference: Map<String, String?> = emptyMap(),
    ) {
        // Get game stats only for available seasons (10 and above, data unavailable for seasons 1-9)
        val availableSeasons = recordStatUtils.getAvailableSeasons()

        val allGameStats =
            gameStatsRepository.findAll().toList()
                .filter { it.season in availableSeasons }

        // Group by team and season, then calculate season totals/averages
        val teamSeasons =
            allGameStats
                .groupBy { "${it.team}_${it.season}" }
                .mapNotNull { (key, stats) ->
                    val value =
                        when {
                            recordStatUtils.lowestOnlyStats.contains(statName) || recordStatUtils.dualRecordStats.contains(statName) -> {
                                recordStatUtils.calculateAverageForStat(statName, stats)
                            }
                            else -> {
                                stats.sumOf { recordStatUtils.getStatValue(statName, it) }
                            }
                        }
                    val (team, seasonStr) = key.split("_")
                    TeamSeason(team, seasonStr.toInt(), value)
                }

        saveBestSeasonRecord(statName, teamSeasons, allGameStats, recordType, RecordScope.LEAGUE, null)
        teamSeasons.groupBy { it.team }.forEach { (team, entries) ->
            saveBestSeasonRecord(statName, entries, allGameStats, recordType, RecordScope.TEAM, team)
        }
        teamSeasons.groupBy { teamConference[it.team] }.forEach { (conference, entries) ->
            if (conference != null) saveBestSeasonRecord(statName, entries, allGameStats, recordType, RecordScope.CONFERENCE, conference)
        }
    }

    private fun saveBestSeasonRecord(
        statName: Stats,
        teamSeasons: List<TeamSeason>,
        allGameStats: List<GameStats>,
        recordType: RecordType,
        recordScope: RecordScope,
        scopeValue: String?,
    ) {
        val isLowest = recordType == RecordType.SINGLE_SEASON_LOWEST
        val best =
            if (isLowest) {
                teamSeasons.minByOrNull { it.value }
            } else {
                teamSeasons.maxByOrNull { it.value }
            } ?: return

        val teamSeasonGameStats = allGameStats.filter { it.team == best.team && it.season == best.season }

        val record =
            Record(
                recordName = statName,
                recordType = recordType,
                recordScope = recordScope,
                scopeValue = scopeValue,
                seasonNumber = best.season,
                week = null,
                gameId = null,
                homeTeam = null,
                awayTeam = null,
                recordTeam = best.team,
                coach = getCoachForSeasonRecord(teamSeasonGameStats),
                recordValue = best.value,
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
        recordScope: RecordScope = RecordScope.LEAGUE,
        scopeValue: String? = null,
    ) {
        val currentRecord = recordRepository.findScopedRecords(statName, recordType, recordScope, scopeValue).firstOrNull()

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
                        recordScope = recordScope,
                        scopeValue = scopeValue,
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
