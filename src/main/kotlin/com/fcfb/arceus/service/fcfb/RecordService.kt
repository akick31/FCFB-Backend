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
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
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

    private fun recordTypesFor(
        stat: Stats,
        highest: RecordType,
        lowest: RecordType,
    ): List<RecordType> =
        when {
            recordStatUtils.lowestOnlyStats.contains(stat) -> listOf(lowest)
            recordStatUtils.dualRecordStats.contains(stat) -> listOf(highest, lowest)
            else -> listOf(highest)
        }

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

        if ((game.week ?: 0) >= POSTSEASON_START_WEEK) {
            val homePostseasonGames = seasonRecordService.getCurrentPostseasonStatsForTeam(game.homeTeam, currentSeason)
            val awayPostseasonGames = seasonRecordService.getCurrentPostseasonStatsForTeam(game.awayTeam, currentSeason)
            runRecordChecks(
                game,
                gameStatsList,
                homePostseasonGames,
                awayPostseasonGames,
                isComplete,
                RecordScope.LEAGUE,
                null,
                gameRecordType = RecordType.SINGLE_POSTSEASON_GAME,
                gameRecordTypeLowest = RecordType.SINGLE_POSTSEASON_GAME_LOWEST,
                seasonRecordType = RecordType.SINGLE_POSTSEASON,
                seasonRecordTypeLowest = RecordType.SINGLE_POSTSEASON_LOWEST,
                checkGeneralRecords = false,
            )
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
        gameRecordType: RecordType = RecordType.SINGLE_GAME,
        gameRecordTypeLowest: RecordType = RecordType.SINGLE_GAME_LOWEST,
        seasonRecordType: RecordType = RecordType.SINGLE_SEASON,
        seasonRecordTypeLowest: RecordType = RecordType.SINGLE_SEASON_LOWEST,
        checkGeneralRecords: Boolean = true,
    ) {
        for (stat in Stats.values()) {
            if (recordStatUtils.againstBaseStat.containsKey(stat)) continue
            if (recordStatUtils.generalRecordStats.contains(stat)) {
                if (checkGeneralRecords && isComplete) {
                    recordTypesFor(stat, RecordType.GENERAL, RecordType.GENERAL_LOWEST).forEach { type ->
                        generalRecordService.checkAndUpdateGeneralRecord(stat, gameStatsList, game, type, recordScope, scopeValue)
                    }
                }
            } else if (recordStatUtils.gameOnlyStats.contains(stat)) {
                if (isComplete) {
                    recordTypesFor(stat, gameRecordType, gameRecordTypeLowest).forEach { type ->
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, type, recordScope, scopeValue)
                    }
                }
            } else {
                if (isComplete) {
                    recordTypesFor(stat, gameRecordType, gameRecordTypeLowest).forEach { type ->
                        gameRecordService.checkAndUpdateGameRecord(stat, gameStatsList, game, type, recordScope, scopeValue)
                    }
                }

                recordTypesFor(stat, seasonRecordType, seasonRecordTypeLowest).forEach { type ->
                    seasonRecordService.checkAndUpdateSeasonRecord(stat, homeSeasonGames, awaySeasonGames, type, recordScope, scopeValue)
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
        val regularSeasonGameStats = allSeasonGameStats.filter { (it.week ?: 0) < POSTSEASON_START_WEEK }
        val postseasonGameStats = allSeasonGameStats.filter { (it.week ?: 0) >= POSTSEASON_START_WEEK }
        val completePostseasonGameStats = postseasonGameStats.filter { it.gameId in completedGameIds }
        val generatePostseason = RecordScope.LEAGUE in scopes
        val postseasonScopes = setOf(RecordScope.LEAGUE)

        val statsToProcess = Stats.values().filterNot { recordStatUtils.againstBaseStat.containsKey(it) }
        statsToProcess.forEachIndexed { index, stat ->
            Logger.info("Generating $stat records for season $seasonNumber (${index + 1}/${statsToProcess.size})")

            if (recordStatUtils.generalRecordStats.contains(stat)) {
                recordTypesFor(stat, RecordType.GENERAL, RecordType.GENERAL_LOWEST).forEach { type ->
                    generalRecordService.generateGeneralRecord(stat, generalGameStats, type, gamesById, teamConference, scopes)
                }
                return@forEachIndexed
            }

            if (recordStatUtils.gameOnlyStats.contains(stat)) {
                recordTypesFor(stat, RecordType.SINGLE_GAME, RecordType.SINGLE_GAME_LOWEST).forEach { type ->
                    gameRecordService.generateGameRecord(stat, completeSeasonGameStats, type, gamesById, teamConference, scopes)
                }
                if (generatePostseason) {
                    recordTypesFor(stat, RecordType.SINGLE_POSTSEASON_GAME, RecordType.SINGLE_POSTSEASON_GAME_LOWEST).forEach { type ->
                        gameRecordService.generateGameRecord(
                            stat,
                            completePostseasonGameStats,
                            type,
                            gamesById,
                            teamConference,
                            postseasonScopes,
                        )
                    }
                }
                return@forEachIndexed
            }

            recordTypesFor(stat, RecordType.SINGLE_GAME, RecordType.SINGLE_GAME_LOWEST).forEach { type ->
                gameRecordService.generateGameRecord(stat, completeSeasonGameStats, type, gamesById, teamConference, scopes)
            }
            if (generatePostseason) {
                recordTypesFor(stat, RecordType.SINGLE_POSTSEASON_GAME, RecordType.SINGLE_POSTSEASON_GAME_LOWEST).forEach { type ->
                    gameRecordService.generateGameRecord(
                        stat,
                        completePostseasonGameStats,
                        type,
                        gamesById,
                        teamConference,
                        postseasonScopes,
                    )
                }
            }

            recordTypesFor(stat, RecordType.SINGLE_SEASON, RecordType.SINGLE_SEASON_LOWEST).forEach { type ->
                seasonRecordService.generateSeasonRecord(stat, regularSeasonGameStats, type, teamConference, scopes)
            }
            if (generatePostseason) {
                recordTypesFor(stat, RecordType.SINGLE_POSTSEASON, RecordType.SINGLE_POSTSEASON_LOWEST).forEach { type ->
                    seasonRecordService.generateSeasonRecord(stat, postseasonGameStats, type, teamConference, postseasonScopes)
                }
            }
        }

        Logger.info("Generating defense (against) records for season $seasonNumber")
        againstRecordService.generateAgainstRecords(
            allSeasonGameStats,
            regularSeasonGameStats,
            postseasonGameStats,
            completeSeasonGameStats,
            completePostseasonGameStats,
            teamConference,
            scopes,
        )
        Logger.info("Completed generating records for season $seasonNumber")
    }
}
