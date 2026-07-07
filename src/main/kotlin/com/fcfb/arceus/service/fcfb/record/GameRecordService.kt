package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service

/**
 * Handles generation and in-flight checking of single-game records.
 */
@Service
class GameRecordService(
    private val recordRepository: RecordRepository,
    private val gameRepository: GameRepository,
    private val recordStatUtils: RecordStatUtils,
) {
    fun generateGameRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        recordType: RecordType,
    ) {
        val isLowest = recordType == RecordType.SINGLE_GAME_LOWEST
        val bestGameStats =
            if (isLowest) {
                gameStatsList.minByOrNull { recordStatUtils.getStatValue(statName, it) }
            } else {
                gameStatsList.maxByOrNull { recordStatUtils.getStatValue(statName, it) }
            } ?: return

        val statValue = recordStatUtils.getStatValue(statName, bestGameStats)

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

    fun checkAndUpdateGameRecord(
        statName: Stats,
        gameStatsList: List<GameStats>,
        game: Game,
        recordType: RecordType,
    ) {
        val currentRecord = recordRepository.findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(statName, recordType)

        for (gameStats in gameStatsList) {
            val currentValue = recordStatUtils.getStatValue(statName, gameStats)
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

    fun getCoachForGameRecord(gameStats: GameStats): String? {
        return gameStats.coaches?.joinToString(", ") ?: null
    }

    fun getHomeTeamForGame(gameId: Int): String {
        return try {
            gameRepository.getGameById(gameId)?.homeTeam ?: ""
        } catch (e: Exception) {
            Logger.error("Error getting home team for game $gameId: ${e.message}", e)
            ""
        }
    }

    fun getAwayTeamForGame(gameId: Int): String {
        return try {
            gameRepository.getGameById(gameId)?.awayTeam ?: ""
        } catch (e: Exception) {
            Logger.error("Error getting away team for game $gameId: ${e.message}", e)
            ""
        }
    }
}
