package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordScope
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service

@Service
class GeneralRecordService(
    private val recordRepository: RecordRepository,
    private val recordStatUtils: RecordStatUtils,
    private val gameRecordService: GameRecordService,
) {
    fun generateGeneralRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
        gamesById: Map<Int, Game> = emptyMap(),
        teamConference: Map<String, String?> = emptyMap(),
        scopes: Set<RecordScope> = RecordScope.ALL,
    ) {
        if (RecordScope.LEAGUE in scopes) {
            saveBestGeneralRecord(statName, gameStatsList, recordType, RecordScope.LEAGUE, null, gamesById)
        }
        if (RecordScope.TEAM in scopes) {
            gameStatsList.groupBy { it.team }.forEach { (team, stats) ->
                if (team != null) saveBestGeneralRecord(statName, stats, recordType, RecordScope.TEAM, team, gamesById)
            }
        }
        if (RecordScope.CONFERENCE in scopes) {
            gameStatsList.groupBy { teamConference[it.team] }.forEach { (conference, stats) ->
                if (conference != null) saveBestGeneralRecord(statName, stats, recordType, RecordScope.CONFERENCE, conference, gamesById)
            }
        }
    }

    private fun saveBestGeneralRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
        recordScope: RecordScope,
        scopeValue: String?,
        gamesById: Map<Int, Game>,
    ) {
        val isLowest = recordType.isLowest
        val bestGameStats =
            if (isLowest) {
                gameStatsList.minByOrNull { recordStatUtils.getStatValue(statName, it) }
            } else {
                gameStatsList.maxByOrNull { recordStatUtils.getStatValue(statName, it) }
            } ?: return

        val game = gamesById[bestGameStats.gameId]

        val record =
            Record(
                recordName = statName,
                recordType = recordType,
                recordScope = recordScope,
                scopeValue = scopeValue,
                seasonNumber = bestGameStats.season ?: 0,
                week = bestGameStats.week ?: 0,
                gameId = bestGameStats.gameId,
                homeTeam = game?.homeTeam ?: gameRecordService.getHomeTeamForGame(bestGameStats.gameId),
                awayTeam = game?.awayTeam ?: gameRecordService.getAwayTeamForGame(bestGameStats.gameId),
                recordTeam = bestGameStats.team ?: "",
                coach = gameRecordService.getCoachForGameRecord(bestGameStats),
                recordValue = recordStatUtils.getStatValue(statName, bestGameStats),
            )

        recordRepository.save(record)
    }

    fun checkAndUpdateGeneralRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        game: Game,
        recordType: RecordType,
        recordScope: RecordScope = RecordScope.LEAGUE,
        scopeValue: String? = null,
    ) {
        val currentRecord = recordRepository.findScopedRecords(statName, recordType, recordScope, scopeValue).firstOrNull()

        for (gameStats in gameStatsList) {
            val currentValue = recordStatUtils.getStatValue(statName, gameStats)
            val recordValue = currentRecord?.recordValue ?: if (recordType.isLowest) Double.MAX_VALUE else 0.0

            val isNewRecord =
                if (recordType.isLowest) {
                    currentValue < recordValue
                } else {
                    currentValue > recordValue
                }

            if (isNewRecord) {
                val newRecord =
                    Record(
                        recordName = statName,
                        recordType = recordType,
                        recordScope = recordScope,
                        scopeValue = scopeValue,
                        seasonNumber = game.season ?: 0,
                        week = game.week ?: 0,
                        gameId = game.gameId,
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        recordTeam = gameStats.team ?: "",
                        coach = gameRecordService.getCoachForGameRecord(gameStats),
                        recordValue = currentValue,
                        previousRecordValue = recordValue,
                        previousRecordTeam = currentRecord?.recordTeam,
                        previousRecordGameId = currentRecord?.gameId,
                    )

                recordRepository.save(newRecord)
                Logger.info("New ${recordType.name} record: ${statName.name} = $currentValue by ${gameStats.team} in game ${game.gameId}")
            }
        }
    }
}
