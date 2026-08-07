package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordScope
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
import org.springframework.stereotype.Service

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
        scopes: Set<RecordScope> = RecordScope.ALL,
    ) {
        val teamSeasons =
            gameStatsList
                .filter { it.team != null }
                .groupBy { it.team!! }
                .map { (team, games) ->
                    TeamSeason(team, games.first().season ?: 0, recordStatUtils.calculateSeasonValue(statName, games))
                }

        if (RecordScope.LEAGUE in scopes) {
            saveBestSeasonRecord(statName, teamSeasons, gameStatsList, recordType, RecordScope.LEAGUE, null)
        }
        if (RecordScope.TEAM in scopes) {
            teamSeasons.groupBy { it.team }.forEach { (team, entries) ->
                saveBestSeasonRecord(statName, entries, gameStatsList, recordType, RecordScope.TEAM, team)
            }
        }
        if (RecordScope.CONFERENCE in scopes) {
            teamSeasons.groupBy { teamConference[it.team] }.forEach { (conference, entries) ->
                if (conference != null) {
                    saveBestSeasonRecord(statName, entries, gameStatsList, recordType, RecordScope.CONFERENCE, conference)
                }
            }
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
        val isLowest = recordType.isLowest
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
    ): List<GameStats> {
        return gameStatsRepository.findAll()
            .filter { it.team == team && it.season == season }
    }

    fun getCurrentPostseasonStatsForTeam(
        team: String,
        season: Int,
    ): List<GameStats> {
        return gameStatsRepository.findAll()
            .filter { it.team == team && it.season == season && (it.week ?: 0) >= POSTSEASON_START_WEEK }
    }

    private data class SeasonCandidate(val team: String, val season: Int, val value: Double, val games: List<GameStats>)

    fun checkAndUpdateSeasonRecord(
        statName: Stats,
        homeSeasonGames: List<GameStats>,
        awaySeasonGames: List<GameStats>,
        recordType: RecordType,
        recordScope: RecordScope = RecordScope.LEAGUE,
        scopeValue: String? = null,
    ) {
        val currentRecord = recordRepository.findScopedRecords(statName, recordType, recordScope, scopeValue).firstOrNull()

        val candidates =
            listOf(homeSeasonGames, awaySeasonGames).mapNotNull { games ->
                val first = games.firstOrNull() ?: return@mapNotNull null
                SeasonCandidate(first.team!!, first.season!!, recordStatUtils.calculateSeasonValue(statName, games), games)
            }

        val isLowest = recordType.isLowest
        val best =
            if (isLowest) {
                candidates.minByOrNull { it.value }
            } else {
                candidates.maxByOrNull { it.value }
            }

        if (best != null) {
            val recordValue = currentRecord?.recordValue ?: if (isLowest) Double.MAX_VALUE else 0.0

            val isNewRecord =
                if (isLowest) {
                    best.value < recordValue
                } else {
                    best.value > recordValue
                }

            if (isNewRecord) {
                val newRecord =
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
                        coach = getCoachForSeasonRecord(best.games),
                        recordValue = best.value,
                        previousRecordValue = recordValue,
                        previousRecordTeam = currentRecord?.recordTeam,
                        previousRecordGameId = currentRecord?.gameId,
                    )

                recordRepository.save(newRecord)
                Logger.info("New ${recordType.name} record: ${statName.name} = ${best.value} by ${best.team} in season ${best.season}")
            }
        }
    }

    private fun getCoachForSeasonRecord(gameStatsList: List<GameStats>): String? {
        if (gameStatsList.isEmpty()) return null

        val coachGameCounts = mutableMapOf<String, Int>()

        for (gameStats in gameStatsList) {
            gameStats.coaches?.forEach { coach ->
                coachGameCounts[coach] = coachGameCounts.getOrDefault(coach, 0) + 1
            }
        }

        return coachGameCounts.maxByOrNull { it.value }?.key
    }
}
