package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service

/**
 * Handles generation and in-flight checking of general (all-time) records,
 * i.e. records that don't need a season/game distinction such as "longest field goal ever".
 */
@Service
class GeneralRecordService(
    private val recordRepository: RecordRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val recordStatUtils: RecordStatUtils,
    private val gameRecordService: GameRecordService,
) {
    fun generateGeneralRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
        completedGameIds: Set<Int>,
    ) {
        // Get game stats only for seasons 10 and 11 (data unavailable for seasons 1-9)
        val allGameStats =
            gameStatsRepository.findAll().toList()
                .filter { (it.season == 10 || it.season == 11) && it.gameId in completedGameIds }

        val isLowest = recordType == RecordType.GENERAL_LOWEST
        val bestGameStats =
            if (isLowest) {
                allGameStats.minByOrNull { recordStatUtils.getStatValue(statName, it) }
            } else {
                allGameStats.maxByOrNull { recordStatUtils.getStatValue(statName, it) }
            } ?: return

        val record =
            Record(
                recordName = statName,
                recordType = recordType,
                seasonNumber = bestGameStats.season ?: 0,
                week = bestGameStats.week ?: 0,
                gameId = bestGameStats.gameId,
                homeTeam = gameRecordService.getHomeTeamForGame(bestGameStats.gameId),
                awayTeam = gameRecordService.getAwayTeamForGame(bestGameStats.gameId),
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
    ) {
        val currentRecord = recordRepository.findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(statName, recordType)

        for (gameStats in gameStatsList) {
            val currentValue = recordStatUtils.getStatValue(statName, gameStats)
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
                        coach = gameRecordService.getCoachForGameRecord(gameStats),
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
}
