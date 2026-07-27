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
        completeSeasonGameStats: List<GameStats>,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
    ) {
        val opponentOf = buildOpponentMap(seasonGameStats)
        recordStatUtils.againstBaseStat.forEach { (againstStat, baseStat) ->
            generateSeasonAgainst(againstStat, baseStat, seasonGameStats, opponentOf, teamConference, scopes)
            generateGameAgainst(againstStat, baseStat, completeSeasonGameStats, opponentOf, teamConference, scopes)
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

    private fun generateSeasonAgainst(
        againstStat: Stats,
        baseStat: Stats,
        seasonGameStats: List<GameStats>,
        opponentOf: Map<GameStats, GameStats>,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
    ) {
        val opponentsByTeam = HashMap<String, MutableList<GameStats>>()
        val ownRowByTeam = HashMap<String, GameStats>()
        seasonGameStats.forEach { row ->
            val opponent = opponentOf[row]
            val team = row.team
            if (opponent != null && team != null) {
                opponentsByTeam.getOrPut(team) { mutableListOf() }.add(opponent)
                ownRowByTeam.putIfAbsent(team, row)
            }
        }
        val teamValues = opponentsByTeam.mapValues { (_, opponents) -> recordStatUtils.calculateSeasonValue(baseStat, opponents) }
        emitBest(againstStat, RecordType.SINGLE_SEASON_LOWEST, teamValues, ownRowByTeam, teamConference, scopes, isSeason = true)
    }

    private fun generateGameAgainst(
        againstStat: Stats,
        baseStat: Stats,
        completeGameStats: List<GameStats>,
        opponentOf: Map<GameStats, GameStats>,
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
        emitBest(againstStat, RecordType.SINGLE_GAME_LOWEST, teamValues, ownRowByTeam, teamConference, scopes, isSeason = false)
    }

    private fun emitBest(
        againstStat: Stats,
        recordType: RecordType,
        teamValues: Map<String, Double>,
        ownRowByTeam: Map<String, GameStats>,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
        isSeason: Boolean,
    ) {
        if (teamValues.isEmpty()) return
        if (RecordScope.LEAGUE in scopes) {
            saveAgainst(againstStat, recordType, teamValues, ownRowByTeam, RecordScope.LEAGUE, null, isSeason)
        }
        if (RecordScope.TEAM in scopes) {
            teamValues.forEach { (team, value) ->
                saveAgainst(againstStat, recordType, mapOf(team to value), ownRowByTeam, RecordScope.TEAM, team, isSeason)
            }
        }
        if (RecordScope.CONFERENCE in scopes) {
            teamValues.entries.groupBy { teamConference[it.key] }.forEach { (conference, entries) ->
                if (conference != null) {
                    saveAgainst(
                        againstStat,
                        recordType,
                        entries.associate { it.key to it.value },
                        ownRowByTeam,
                        RecordScope.CONFERENCE,
                        conference,
                        isSeason,
                    )
                }
            }
        }
    }

    private fun saveAgainst(
        againstStat: Stats,
        recordType: RecordType,
        teamValues: Map<String, Double>,
        ownRowByTeam: Map<String, GameStats>,
        recordScope: RecordScope,
        scopeValue: String?,
        isSeason: Boolean,
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
                week = if (isSeason) null else ownRow.week ?: 0,
                gameId = if (isSeason) null else ownRow.gameId,
                homeTeam = null,
                awayTeam = null,
                recordTeam = best.key,
                coach = gameRecordService.getCoachForGameRecord(ownRow),
                recordValue = best.value,
            )
        recordRepository.save(record)
    }
}
