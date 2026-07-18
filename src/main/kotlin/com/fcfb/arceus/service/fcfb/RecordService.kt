package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.service.fcfb.record.GameRecordService
import com.fcfb.arceus.service.fcfb.record.GeneralRecordService
import com.fcfb.arceus.service.fcfb.record.RecordStatUtils
import com.fcfb.arceus.service.fcfb.record.SeasonRecordService
import com.fcfb.arceus.service.specification.RecordSpecificationService
import com.fcfb.arceus.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class RecordService(
    private val recordRepository: RecordRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val recordSpecificationService: RecordSpecificationService,
    private val gameRecordService: GameRecordService,
    private val seasonRecordService: SeasonRecordService,
    private val generalRecordService: GeneralRecordService,
    private val recordStatUtils: RecordStatUtils,
) {
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

    fun generateAllRecords() {
        Logger.info("Starting generation of all records")

        // Clear existing records
        recordRepository.deleteAll()

        // Pre-compute completed game IDs once for the entire generation
        val completedGameIds = recordStatUtils.getCompletedGameIds()

        // Get all available seasons (10 and above, since data unavailable for seasons 1-9)
        val availableSeasons = recordStatUtils.getAvailableSeasons()

        for (season in availableSeasons) {
            generateRecordsForSeason(season, completedGameIds)
        }

        Logger.info("Completed generation of all records")
    }

    /**
     * Check if a game broke any records and update them.
     * Only considers games that completed at least 4 quarters.
     */
    fun checkAndUpdateRecordsForGame(game: Game) {
        val isComplete = recordStatUtils.isCompleteGame(game)
        if (!isComplete) {
            Logger.info(
                "Game ${game.gameId} did not fully complete (quarter: ${game.quarter}, clock: ${game.clock}) " +
                    "- skipping single game/general records, still checking season records",
            )
        }
        Logger.info("Checking records for game ${game.gameId}")

        val homeStats = gameStatsRepository.getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
        val awayStats = gameStatsRepository.getGameStatsByIdAndTeam(game.gameId, game.awayTeam)

        val gameStatsList = listOfNotNull(homeStats, awayStats)

        // Get current season stats for both teams (for season record checking)
        val currentSeason = game.season ?: return
        val homeSeasonStats = seasonRecordService.getCurrentSeasonStatsForTeam(game.homeTeam, currentSeason)
        val awaySeasonStats = seasonRecordService.getCurrentSeasonStatsForTeam(game.awayTeam, currentSeason)

        // Check each stat type
        for (stat in Stats.values()) {
            if (recordStatUtils.generalRecordStats.contains(stat)) {
                // General records - only check for complete games
                if (isComplete) {
                    if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                        generalRecordService.checkAndUpdateGeneralRecord(stat, gameStatsList, game, RecordType.GENERAL_LOWEST)
                    } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                        generalRecordService.checkAndUpdateGeneralRecord(stat, gameStatsList, game, RecordType.GENERAL)
                        generalRecordService.checkAndUpdateGeneralRecord(stat, gameStatsList, game, RecordType.GENERAL_LOWEST)
                    } else {
                        generalRecordService.checkAndUpdateGeneralRecord(stat, gameStatsList, game, RecordType.GENERAL)
                    }
                }
            } else if (recordStatUtils.gameOnlyStats.contains(stat)) {
                // Game-only records - only check for complete games
                if (isComplete) {
                    if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME_LOWEST)
                    } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME)
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME_LOWEST)
                    } else {
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME)
                    }
                }
            } else {
                // Regular stats - game records only for complete games, season records always
                if (isComplete) {
                    if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME_LOWEST)
                    } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME)
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME_LOWEST)
                    } else {
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, RecordType.SINGLE_GAME)
                    }
                }

                // Season records always checked regardless of game completion
                if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                    seasonRecordService.checkAndUpdateSeasonRecord(stat, homeSeasonStats, awaySeasonStats, RecordType.SINGLE_SEASON_LOWEST)
                } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                    seasonRecordService.checkAndUpdateSeasonRecord(stat, homeSeasonStats, awaySeasonStats, RecordType.SINGLE_SEASON)
                    seasonRecordService.checkAndUpdateSeasonRecord(stat, homeSeasonStats, awaySeasonStats, RecordType.SINGLE_SEASON_LOWEST)
                } else {
                    seasonRecordService.checkAndUpdateSeasonRecord(stat, homeSeasonStats, awaySeasonStats, RecordType.SINGLE_SEASON)
                }
            }
        }

        Logger.info("Completed checking records for game ${game.gameId}")
    }

    private fun generateRecordsForSeason(
        seasonNumber: Int,
        completedGameIds: Set<Int>,
    ) {
        Logger.info("Generating records for season $seasonNumber")

        val allSeasonGameStats =
            gameStatsRepository.findAll()
                .filter { it.season == seasonNumber }

        // Only complete games for single game and general records
        val completeSeasonGameStats = allSeasonGameStats.filter { it.gameId in completedGameIds }

        for (stat in Stats.values()) {
            if (recordStatUtils.generalRecordStats.contains(stat)) {
                // General records - only complete games
                if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                    generalRecordService.generateGeneralRecord(stat, completeSeasonGameStats, RecordType.GENERAL_LOWEST, completedGameIds)
                } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                    generalRecordService.generateGeneralRecord(stat, completeSeasonGameStats, RecordType.GENERAL, completedGameIds)
                    generalRecordService.generateGeneralRecord(stat, completeSeasonGameStats, RecordType.GENERAL_LOWEST, completedGameIds)
                } else {
                    generalRecordService.generateGeneralRecord(stat, completeSeasonGameStats, RecordType.GENERAL, completedGameIds)
                }
            } else if (recordStatUtils.gameOnlyStats.contains(stat)) {
                // Game-only records - only complete games
                if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, RecordType.SINGLE_GAME_LOWEST)
                } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, RecordType.SINGLE_GAME)
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, RecordType.SINGLE_GAME_LOWEST)
                } else {
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, RecordType.SINGLE_GAME)
                }
            } else {
                // Regular stats - game records use complete games only, season records use all games
                if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, RecordType.SINGLE_GAME_LOWEST)
                    seasonRecordService.generateSeasonRecord(stat, allSeasonGameStats, RecordType.SINGLE_SEASON_LOWEST)
                } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, RecordType.SINGLE_GAME)
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, RecordType.SINGLE_GAME_LOWEST)
                    seasonRecordService.generateSeasonRecord(stat, allSeasonGameStats, RecordType.SINGLE_SEASON)
                    seasonRecordService.generateSeasonRecord(stat, allSeasonGameStats, RecordType.SINGLE_SEASON_LOWEST)
                } else {
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, RecordType.SINGLE_GAME)
                    seasonRecordService.generateSeasonRecord(stat, allSeasonGameStats, RecordType.SINGLE_SEASON)
                }
            }
        }
    }
}
