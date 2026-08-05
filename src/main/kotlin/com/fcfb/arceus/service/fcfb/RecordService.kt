package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.records.RecordScope
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.fcfb.record.AgainstRecordService
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
    private val gameRepository: GameRepository,
    private val recordSpecificationService: RecordSpecificationService,
    private val gameRecordService: GameRecordService,
    private val seasonRecordService: SeasonRecordService,
    private val generalRecordService: GeneralRecordService,
    private val againstRecordService: AgainstRecordService,
    private val recordStatUtils: RecordStatUtils,
    private val teamRepository: TeamRepository,
) {
    private fun conferenceOf(team: String?): String? =
        try {
            teamRepository.getTeamByName(team)?.conference
        } catch (e: Exception) {
            Logger.error("Error looking up conference for team $team: ${e.message}")
            null
        }

    private fun buildTeamConferenceMap(teams: Collection<String?>): Map<String, String?> =
        teams.filterNotNull().distinct().associateWith { conferenceOf(it) }

    fun getFilteredRecords(
        season: Int?,
        recordType: RecordType?,
        recordName: Stats?,
        recordScope: RecordScope?,
        scopeValue: String?,
        pageable: Pageable,
    ): Page<Record> {
        val spec = recordSpecificationService.createSpecification(season, recordType, recordName, recordScope, scopeValue)
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

        recordRepository.deleteAll()

        val completedGameIds = recordStatUtils.getCompletedGameIds()

        val availableSeasons = recordStatUtils.getAvailableSeasons()
        val availableSeasonsSet = availableSeasons.toSet()

        val allGameStats = gameStatsRepository.findAll().toList()
        val gamesById = gameRepository.findAll().associateBy { it.gameId }
        val teamConference = buildTeamConferenceMap(allGameStats.map { it.team })
        val generalGameStats = allGameStats.filter { it.season in availableSeasonsSet && it.gameId in completedGameIds }

        for (season in availableSeasons) {
            generateRecordsForSeason(season, completedGameIds, teamConference, allGameStats, gamesById, generalGameStats)
        }

        logRecordCountBreakdown("Completed generation of all records", RecordScope.ALL)
    }

    private fun logRecordCountBreakdown(
        prefix: String,
        scopes: Set<RecordScope>,
    ) {
        val breakdown =
            scopes.sorted().joinToString(", ") { scope ->
                "${recordRepository.countByRecordScope(scope)} ${scope.name.lowercase()} records"
            }
        Logger.info("$prefix: $breakdown")
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

        val currentSeason = game.season ?: return
        val homeSeasonGames = seasonRecordService.getCurrentSeasonStatsForTeam(game.homeTeam, currentSeason)
        val awaySeasonGames = seasonRecordService.getCurrentSeasonStatsForTeam(game.awayTeam, currentSeason)

        runRecordChecks(game, gameStatsList, homeSeasonGames, awaySeasonGames, isComplete, RecordScope.LEAGUE, null)

        listOf(homeStats to game.homeTeam, awayStats to game.awayTeam).forEach { (teamGameStats, teamName) ->
            val teamStatsList = listOfNotNull(teamGameStats)
            val teamSeasonGames = if (teamName == game.homeTeam) homeSeasonGames else awaySeasonGames
            runRecordChecks(game, teamStatsList, teamSeasonGames, emptyList(), isComplete, RecordScope.TEAM, teamName)
            conferenceOf(teamName)?.let { conference ->
                runRecordChecks(game, teamStatsList, teamSeasonGames, emptyList(), isComplete, RecordScope.CONFERENCE, conference)
            }
        }

        Logger.info("Completed checking records for game ${game.gameId}")
    }

    private fun runRecordChecks(
        game: Game,
        gameStatsList: List<GameStats>,
        homeSeasonGames: List<GameStats>,
        awaySeasonGames: List<GameStats>,
        isComplete: Boolean,
        recordScope: RecordScope,
        scopeValue: String?,
    ) {
        for (stat in Stats.values()) {
            if (recordStatUtils.againstBaseStat.containsKey(stat)) continue
            if (recordStatUtils.generalRecordStats.contains(stat)) {
                if (isComplete) {
                    if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                        generalRecordService.checkAndUpdateGeneralRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.GENERAL_LOWEST,
                            recordScope,
                            scopeValue,
                        )
                    } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                        generalRecordService.checkAndUpdateGeneralRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.GENERAL,
                            recordScope,
                            scopeValue,
                        )
                        generalRecordService.checkAndUpdateGeneralRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.GENERAL_LOWEST,
                            recordScope,
                            scopeValue,
                        )
                    } else {
                        generalRecordService.checkAndUpdateGeneralRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.GENERAL,
                            recordScope,
                            scopeValue,
                        )
                    }
                }
            } else if (recordStatUtils.gameOnlyStats.contains(stat)) {
                if (isComplete) {
                    if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                        gameRecordService.checkAndUpdateGameRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.SINGLE_GAME_LOWEST,
                            recordScope,
                            scopeValue,
                        )
                    } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                        gameRecordService.checkAndUpdateGameRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.SINGLE_GAME,
                            recordScope,
                            scopeValue,
                        )
                        gameRecordService.checkAndUpdateGameRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.SINGLE_GAME_LOWEST,
                            recordScope,
                            scopeValue,
                        )
                    } else {
                        gameRecordService.checkAndUpdateGameRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.SINGLE_GAME,
                            recordScope,
                            scopeValue,
                        )
                    }
                }
            } else {
                if (isComplete) {
                    if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                        gameRecordService.checkAndUpdateGameRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.SINGLE_GAME_LOWEST,
                            recordScope,
                            scopeValue,
                        )
                    } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                        gameRecordService.checkAndUpdateGameRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.SINGLE_GAME,
                            recordScope,
                            scopeValue,
                        )
                        gameRecordService.checkAndUpdateGameRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.SINGLE_GAME_LOWEST,
                            recordScope,
                            scopeValue,
                        )
                    } else {
                        gameRecordService.checkAndUpdateGameRecord(
                            stat,
                            gameStatsList,
                            game,
                            RecordType.SINGLE_GAME,
                            recordScope,
                            scopeValue,
                        )
                    }
                }

                if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                    seasonRecordService.checkAndUpdateSeasonRecord(
                        stat,
                        homeSeasonGames,
                        awaySeasonGames,
                        RecordType.SINGLE_SEASON_LOWEST,
                        recordScope,
                        scopeValue,
                    )
                } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                    seasonRecordService.checkAndUpdateSeasonRecord(
                        stat,
                        homeSeasonGames,
                        awaySeasonGames,
                        RecordType.SINGLE_SEASON,
                        recordScope,
                        scopeValue,
                    )
                    seasonRecordService.checkAndUpdateSeasonRecord(
                        stat,
                        homeSeasonGames,
                        awaySeasonGames,
                        RecordType.SINGLE_SEASON_LOWEST,
                        recordScope,
                        scopeValue,
                    )
                } else {
                    seasonRecordService.checkAndUpdateSeasonRecord(
                        stat,
                        homeSeasonGames,
                        awaySeasonGames,
                        RecordType.SINGLE_SEASON,
                        recordScope,
                        scopeValue,
                    )
                }
            }
        }
    }

    fun generateTeamAndConferenceRecords() {
        Logger.info("Starting generation of team and conference records")
        recordRepository.deleteByRecordScopeIn(RecordScope.TEAM_AND_CONFERENCE.toList())
        val completedGameIds = recordStatUtils.getCompletedGameIds()
        val availableSeasons = recordStatUtils.getAvailableSeasons()
        val availableSeasonsSet = availableSeasons.toSet()
        val allGameStats = gameStatsRepository.findAll().toList()
        val gamesById = gameRepository.findAll().associateBy { it.gameId }
        val teamConference = buildTeamConferenceMap(allGameStats.map { it.team })
        val generalGameStats = allGameStats.filter { it.season in availableSeasonsSet && it.gameId in completedGameIds }
        for (season in availableSeasons) {
            generateRecordsForSeason(
                season,
                completedGameIds,
                teamConference,
                allGameStats,
                gamesById,
                generalGameStats,
                RecordScope.TEAM_AND_CONFERENCE,
            )
        }
        logRecordCountBreakdown("Completed generation of team and conference records", RecordScope.TEAM_AND_CONFERENCE)
    }

    private fun generateRecordsForSeason(
        seasonNumber: Int,
        completedGameIds: Set<Int>,
        teamConference: Map<String, String?>,
        allGameStats: List<GameStats>,
        gamesById: Map<Int, Game>,
        generalGameStats: List<GameStats>,
        scopes: Set<RecordScope> = RecordScope.ALL,
    ) {
        Logger.info("Generating records for season $seasonNumber")

        val allSeasonGameStats = allGameStats.filter { it.season == seasonNumber }

        val completeSeasonGameStats = allSeasonGameStats.filter { it.gameId in completedGameIds }

        val statsToProcess = Stats.values().filterNot { recordStatUtils.againstBaseStat.containsKey(it) }
        statsToProcess.forEachIndexed { index, stat ->
            Logger.info("Generating $stat records for season $seasonNumber (${index + 1}/${statsToProcess.size})")
            if (recordStatUtils.generalRecordStats.contains(stat)) {
                if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                    generalRecordService.generateGeneralRecord(
                        stat,
                        generalGameStats,
                        RecordType.GENERAL_LOWEST,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                    generalRecordService.generateGeneralRecord(
                        stat,
                        generalGameStats,
                        RecordType.GENERAL,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                    generalRecordService.generateGeneralRecord(
                        stat,
                        generalGameStats,
                        RecordType.GENERAL_LOWEST,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                } else {
                    generalRecordService.generateGeneralRecord(
                        stat,
                        generalGameStats,
                        RecordType.GENERAL,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                }
            } else if (recordStatUtils.gameOnlyStats.contains(stat)) {
                if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                    gameRecordService.generateGameRecord(
                        stat,
                        completeSeasonGameStats,
                        RecordType.SINGLE_GAME_LOWEST,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                    gameRecordService.generateGameRecord(
                        stat,
                        completeSeasonGameStats,
                        RecordType.SINGLE_GAME,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                    gameRecordService.generateGameRecord(
                        stat,
                        completeSeasonGameStats,
                        RecordType.SINGLE_GAME_LOWEST,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                } else {
                    gameRecordService.generateGameRecord(
                        stat,
                        completeSeasonGameStats,
                        RecordType.SINGLE_GAME,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                }
            } else {
                if (recordStatUtils.lowestOnlyStats.contains(stat)) {
                    gameRecordService.generateGameRecord(
                        stat,
                        completeSeasonGameStats,
                        RecordType.SINGLE_GAME_LOWEST,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                    seasonRecordService.generateSeasonRecord(
                        stat,
                        allSeasonGameStats,
                        RecordType.SINGLE_SEASON_LOWEST,
                        teamConference,
                        scopes,
                    )
                } else if (recordStatUtils.dualRecordStats.contains(stat)) {
                    gameRecordService.generateGameRecord(
                        stat,
                        completeSeasonGameStats,
                        RecordType.SINGLE_GAME,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                    gameRecordService.generateGameRecord(
                        stat,
                        completeSeasonGameStats,
                        RecordType.SINGLE_GAME_LOWEST,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                    seasonRecordService.generateSeasonRecord(stat, allSeasonGameStats, RecordType.SINGLE_SEASON, teamConference, scopes)
                    seasonRecordService.generateSeasonRecord(
                        stat,
                        allSeasonGameStats,
                        RecordType.SINGLE_SEASON_LOWEST,
                        teamConference,
                        scopes,
                    )
                } else if (recordStatUtils.percentageStats.contains(stat)) {
                    seasonRecordService.generateSeasonRecord(stat, allSeasonGameStats, RecordType.SINGLE_SEASON, teamConference, scopes)
                } else {
                    gameRecordService.generateGameRecord(
                        stat,
                        completeSeasonGameStats,
                        RecordType.SINGLE_GAME,
                        gamesById,
                        teamConference,
                        scopes,
                    )
                    seasonRecordService.generateSeasonRecord(stat, allSeasonGameStats, RecordType.SINGLE_SEASON, teamConference, scopes)
                }
            }
        }

        Logger.info("Generating defense (against) records for season $seasonNumber")
        againstRecordService.generateAgainstRecords(allSeasonGameStats, completeSeasonGameStats, teamConference, scopes)
        Logger.info("Completed generating records for season $seasonNumber")
    }
}
