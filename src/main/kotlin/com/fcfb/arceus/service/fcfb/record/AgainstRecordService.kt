package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordScope
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.RecordRepository
import org.springframework.stereotype.Service

@Service
class AgainstRecordService(
    private val recordRepository: RecordRepository,
    private val recordStatUtils: RecordStatUtils,
    private val gameRecordService: GameRecordService,
) {
    fun generateAgainstRecords(
        seasonGameStats: List<GameStats>,
        regularSeasonGameStats: List<GameStats>,
        postseasonGameStats: List<GameStats>,
        completeSeasonGameStats: List<GameStats>,
        completePostseasonGameStats: List<GameStats>,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
    ) {
        val opponentOf = buildOpponentMap(seasonGameStats)
        val postseasonScopes = setOf(RecordScope.LEAGUE)
        recordStatUtils.againstBaseStat.forEach { (againstStat, baseStat) ->
            generateSeasonAgainst(
                againstStat,
                baseStat,
                regularSeasonGameStats,
                opponentOf,
                RecordType.SINGLE_SEASON_LOWEST,
                teamConference,
                scopes,
            )
            generateGameAgainst(
                againstStat,
                baseStat,
                completeSeasonGameStats,
                opponentOf,
                RecordType.SINGLE_GAME_LOWEST,
                teamConference,
                scopes,
            )
            if (RecordScope.LEAGUE in scopes) {
                generateSeasonAgainst(
                    againstStat,
                    baseStat,
                    postseasonGameStats,
                    opponentOf,
                    RecordType.SINGLE_POSTSEASON_LOWEST,
                    teamConference,
                    postseasonScopes,
                )
                generateGameAgainst(
                    againstStat,
                    baseStat,
                    completePostseasonGameStats,
                    opponentOf,
                    RecordType.SINGLE_POSTSEASON_GAME_LOWEST,
                    teamConference,
                    postseasonScopes,
                )
            }
        }
    }

    private fun buildOpponentMap(gameStats: List<GameStats>): Map<GameStats, GameStats> {
        val map = HashMap<GameStats, GameStats>()
        gameStats.groupBy { it.gameId }.values.forEach { rows ->
            if (rows.size == 2) {
                map[rows[0]] = rows[1]
                map[rows[1]] = rows[0]
            }
        }
        return map
    }

    private data class TeamSeasonAgainst(val team: String, val season: Int, val value: Double, val ownRow: GameStats)

    private fun generateSeasonAgainst(
        againstStat: Stats,
        baseStat: Stats,
        seasonGameStats: List<GameStats>,
        opponentOf: Map<GameStats, GameStats>,
        recordType: RecordType,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
    ) {
        val opponentsByTeamSeason = HashMap<Pair<String, Int>, MutableList<GameStats>>()
        val ownRowByTeamSeason = HashMap<Pair<String, Int>, GameStats>()
        seasonGameStats.forEach { row ->
            val opponent = opponentOf[row]
            val team = row.team
            val season = row.season
            if (opponent != null && team != null && season != null) {
                val key = team to season
                opponentsByTeamSeason.getOrPut(key) { mutableListOf() }.add(opponent)
                ownRowByTeamSeason.putIfAbsent(key, row)
            }
        }
        val entries =
            opponentsByTeamSeason.map { (key, opponents) ->
                TeamSeasonAgainst(
                    key.first,
                    key.second,
                    recordStatUtils.calculateSeasonValue(baseStat, opponents),
                    ownRowByTeamSeason.getValue(key),
                )
            }
        emitBestSeasonAgainst(againstStat, recordType, entries, teamConference, scopes)
    }

    private fun emitBestSeasonAgainst(
        againstStat: Stats,
        recordType: RecordType,
        entries: List<TeamSeasonAgainst>,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
    ) {
        if (entries.isEmpty()) return
        if (RecordScope.LEAGUE in scopes) {
            saveBestSeasonAgainst(againstStat, recordType, entries, RecordScope.LEAGUE, null)
        }
        if (RecordScope.TEAM in scopes) {
            entries.groupBy { it.team }.forEach { (team, teamEntries) ->
                saveBestSeasonAgainst(againstStat, recordType, teamEntries, RecordScope.TEAM, team)
            }
        }
        if (RecordScope.CONFERENCE in scopes) {
            entries.groupBy { teamConference[it.team] }.forEach { (conference, confEntries) ->
                if (conference != null) {
                    saveBestSeasonAgainst(againstStat, recordType, confEntries, RecordScope.CONFERENCE, conference)
                }
            }
        }
    }

    private fun saveBestSeasonAgainst(
        againstStat: Stats,
        recordType: RecordType,
        entries: List<TeamSeasonAgainst>,
        recordScope: RecordScope,
        scopeValue: String?,
    ) {
        val best = entries.minByOrNull { it.value } ?: return
        val record =
            Record(
                recordName = againstStat,
                recordType = recordType,
                recordScope = recordScope,
                scopeValue = scopeValue,
                seasonNumber = best.season,
                week = null,
                gameId = null,
                homeTeam = null,
                awayTeam = null,
                recordTeam = best.team,
                coach = gameRecordService.getCoachForGameRecord(best.ownRow),
                recordValue = best.value,
            )
        recordRepository.save(record)
    }

    private fun generateGameAgainst(
        againstStat: Stats,
        baseStat: Stats,
        completeGameStats: List<GameStats>,
        opponentOf: Map<GameStats, GameStats>,
        recordType: RecordType,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
    ) {
        val bestGameByTeam = HashMap<String, Pair<Double, GameStats>>()
        completeGameStats.forEach { row ->
            val opponent = opponentOf[row]
            val team = row.team
            if (opponent != null && team != null) {
                val value = recordStatUtils.getStatValue(baseStat, opponent)
                val currentBest = bestGameByTeam[team]
                if (currentBest == null || value < currentBest.first) {
                    bestGameByTeam[team] = value to row
                }
            }
        }
        val teamValues = bestGameByTeam.mapValues { it.value.first }
        val ownRowByTeam = bestGameByTeam.mapValues { it.value.second }
        emitBestGameAgainst(againstStat, recordType, teamValues, ownRowByTeam, teamConference, scopes)
    }

    private fun emitBestGameAgainst(
        againstStat: Stats,
        recordType: RecordType,
        teamValues: Map<String, Double>,
        ownRowByTeam: Map<String, GameStats>,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
    ) {
        if (teamValues.isEmpty()) return
        if (RecordScope.LEAGUE in scopes) {
            saveGameAgainst(againstStat, recordType, teamValues, ownRowByTeam, RecordScope.LEAGUE, null)
        }
        if (RecordScope.TEAM in scopes) {
            teamValues.forEach { (team, value) ->
                saveGameAgainst(againstStat, recordType, mapOf(team to value), ownRowByTeam, RecordScope.TEAM, team)
            }
        }
        if (RecordScope.CONFERENCE in scopes) {
            teamValues.entries.groupBy { teamConference[it.key] }.forEach { (conference, entries) ->
                if (conference != null) {
                    saveGameAgainst(
                        againstStat,
                        recordType,
                        entries.associate { it.key to it.value },
                        ownRowByTeam,
                        RecordScope.CONFERENCE,
                        conference,
                    )
                }
            }
        }
    }

    private fun saveGameAgainst(
        againstStat: Stats,
        recordType: RecordType,
        teamValues: Map<String, Double>,
        ownRowByTeam: Map<String, GameStats>,
        recordScope: RecordScope,
        scopeValue: String?,
    ) {
        val best = teamValues.entries.minByOrNull { it.value } ?: return
        val ownRow = ownRowByTeam[best.key] ?: return
        val record =
            Record(
                recordName = againstStat,
                recordType = recordType,
                recordScope = recordScope,
                scopeValue = scopeValue,
                seasonNumber = ownRow.season ?: 0,
                week = ownRow.week ?: 0,
                gameId = ownRow.gameId,
                homeTeam = null,
                awayTeam = null,
                recordTeam = best.key,
                coach = gameRecordService.getCoachForGameRecord(ownRow),
                recordValue = best.value,
            )
        recordRepository.save(record)
    }
}
