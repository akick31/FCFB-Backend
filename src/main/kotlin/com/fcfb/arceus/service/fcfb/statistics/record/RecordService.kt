package com.fcfb.arceus.service.fcfb.statistics.record

import com.fcfb.arceus.domain.season.SeasonStructure
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.records.RecordScope
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.exception.statistics.RecordGenerationInProgressException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.record.Record
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.record.RecordRepository
import com.fcfb.arceus.repositories.season.SeasonRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.specification.RecordSpecificationService
import com.fcfb.arceus.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.atomic.AtomicBoolean

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
    private val recordStatValueService: RecordStatValueService,
    private val recordQualificationService: RecordQualificationService,
    private val teamRepository: TeamRepository,
    private val seasonRepository: SeasonRepository,
) {
    private val generationInProgress = AtomicBoolean(false)

    private fun conferenceOf(team: String?): String? =
        try {
            teamRepository.getTeamByName(team)?.conference
        } catch (e: Exception) {
            Logger().error("Error looking up conference for team $team: ${e.message}")
            null
        }

    private fun buildTeamConferenceMap(teams: Collection<String?>): Map<String, String?> {
        val wanted = teams.filterNotNull().distinct()
        if (wanted.isEmpty()) {
            return emptyMap()
        }
        val conferenceByName =
            teamRepository.findAll().mapNotNull { team -> team.name?.let { it to team.conference } }.toMap()
        return wanted.associateWith { conferenceByName[it] }
    }

    private fun unfinishedSeason(): Int? = seasonRepository.getCurrentSeason()?.seasonNumber

    /** Scrimmages and the handful of pre-launch seasons in the database are not real league play and cannot hold records. */
    private fun recordEligibleStats(
        allGameStats: List<GameStats>,
        gamesById: Map<Int, Game>,
    ): List<GameStats> =
        allGameStats.filter { stats ->
            (stats.season ?: 0) >= RecordStatValueService.EARLIEST_VALID_SEASON &&
                gamesById[stats.gameId]?.gameType != GameType.SCRIMMAGE
        }

    private fun recordTypesFor(
        stat: Stats,
        highest: RecordType,
        lowest: RecordType,
    ): List<RecordType> =
        when {
            recordStatValueService.lowestOnlyStats.contains(stat) -> listOf(lowest)
            recordStatValueService.dualRecordStats.contains(stat) -> listOf(highest, lowest)
            else -> listOf(highest)
        }.filter { recordQualificationService.allowsRecordType(stat, it) }

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

    @Transactional(rollbackFor = [Exception::class])
    fun generateAllRecords() {
        if (!generationInProgress.compareAndSet(false, true)) {
            throw RecordGenerationInProgressException()
        }
        try {
            Logger().info("Starting generation of all records")

            recordRepository.deleteAll()

            val completedGameIds = recordStatValueService.getCompletedGameIds()

            val gamesById = gameRepository.findAll().associateBy { it.gameId }
            val eligibleGameStats = recordEligibleStats(gameStatsRepository.findAll().toList(), gamesById)
            val teamConference = buildTeamConferenceMap(eligibleGameStats.map { it.team })
            val generalGameStats = eligibleGameStats.filter { it.gameId in completedGameIds }

            generateGeneralRecords(generalGameStats, gamesById, teamConference, RecordScope.ALL)
            generateHistoryWideRecords(eligibleGameStats, completedGameIds, teamConference, gamesById, RecordScope.ALL)

            logRecordCountBreakdown("Completed generation of all records", RecordScope.ALL)
        } finally {
            generationInProgress.set(false)
        }
    }

    private fun generateGeneralRecords(
        generalGameStats: List<GameStats>,
        gamesById: Map<Int, Game>,
        teamConference: Map<String, String?>,
        scopes: Set<RecordScope>,
    ) {
        val generalStats = Stats.values().filter { recordStatValueService.generalRecordStats.contains(it) }
        generalStats.forEachIndexed { index, stat ->
            Logger().info("Generating $stat general records (${index + 1}/${generalStats.size})")
            recordTypesFor(stat, RecordType.GENERAL, RecordType.GENERAL_LOWEST).forEach { type ->
                generalRecordService.generateGeneralRecord(stat, generalGameStats, type, gamesById, teamConference, scopes)
            }
        }
    }

    private fun logRecordCountBreakdown(
        prefix: String,
        scopes: Set<RecordScope>,
    ) {
        val breakdown =
            scopes.sorted().joinToString(", ") { scope ->
                "${recordRepository.countByRecordScope(scope)} ${scope.name.lowercase()} records"
            }
        Logger().info("$prefix: $breakdown")
    }

    fun checkAndUpdateRecordsForGame(game: Game) {
        if (game.gameType == GameType.SCRIMMAGE || (game.season ?: 0) < RecordStatValueService.EARLIEST_VALID_SEASON) {
            Logger().info("Game ${game.gameId} is not eligible for records - skipping record checks")
            return
        }
        val isComplete = recordStatValueService.isCompleteGame(game)
        if (!isComplete) {
            Logger().info(
                "Game ${game.gameId} did not fully complete (quarter: ${game.quarter}, clock: ${game.clock}) " +
                    "- skipping single game/general records, still checking season records",
            )
        }
        Logger().info("Checking records for game ${game.gameId}")

        val homeStats = gameStatsRepository.getGameStatsByIdAndTeam(game.gameId, game.homeTeam)
        val awayStats = gameStatsRepository.getGameStatsByIdAndTeam(game.gameId, game.awayTeam)

        val gameStatsList = listOfNotNull(homeStats, awayStats)

        val currentSeason = game.season ?: return
        val homeSeasonGames = seasonRecordService.getCurrentSeasonStatsForTeam(game.homeTeam, currentSeason)
        val awaySeasonGames = seasonRecordService.getCurrentSeasonStatsForTeam(game.awayTeam, currentSeason)

        val seasonComplete = currentSeason != unfinishedSeason()

        runRecordChecks(game, gameStatsList, homeSeasonGames, awaySeasonGames, isComplete, RecordScope.LEAGUE, null, seasonComplete)

        listOf(homeStats to game.homeTeam, awayStats to game.awayTeam).forEach { (teamGameStats, teamName) ->
            val teamStatsList = listOfNotNull(teamGameStats)
            val teamSeasonGames = if (teamName == game.homeTeam) homeSeasonGames else awaySeasonGames
            runRecordChecks(game, teamStatsList, teamSeasonGames, emptyList(), isComplete, RecordScope.TEAM, teamName, seasonComplete)
            conferenceOf(teamName)?.let { conference ->
                runRecordChecks(
                    game,
                    teamStatsList,
                    teamSeasonGames,
                    emptyList(),
                    isComplete,
                    RecordScope.CONFERENCE,
                    conference,
                    seasonComplete,
                )
            }
        }

        if ((game.week ?: 0) >= SeasonStructure.POSTSEASON_START_WEEK) {
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
                seasonComplete,
                gameRecordType = RecordType.SINGLE_POSTSEASON_GAME,
                gameRecordTypeLowest = RecordType.SINGLE_POSTSEASON_GAME_LOWEST,
                seasonRecordType = RecordType.SINGLE_POSTSEASON,
                seasonRecordTypeLowest = RecordType.SINGLE_POSTSEASON_LOWEST,
                checkGeneralRecords = false,
            )
        }

        Logger().info("Completed checking records for game ${game.gameId}")
    }

    private fun runRecordChecks(
        game: Game,
        gameStatsList: List<GameStats>,
        homeSeasonGames: List<GameStats>,
        awaySeasonGames: List<GameStats>,
        isComplete: Boolean,
        recordScope: RecordScope,
        scopeValue: String?,
        seasonComplete: Boolean,
        gameRecordType: RecordType = RecordType.SINGLE_GAME,
        gameRecordTypeLowest: RecordType = RecordType.SINGLE_GAME_LOWEST,
        seasonRecordType: RecordType = RecordType.SINGLE_SEASON,
        seasonRecordTypeLowest: RecordType = RecordType.SINGLE_SEASON_LOWEST,
        checkGeneralRecords: Boolean = true,
    ) {
        for (stat in Stats.values()) {
            if (recordStatValueService.againstBaseStat.containsKey(stat)) continue
            if (recordStatValueService.generalRecordStats.contains(stat)) {
                if (checkGeneralRecords && isComplete) {
                    recordTypesFor(stat, RecordType.GENERAL, RecordType.GENERAL_LOWEST).forEach { type ->
                        generalRecordService.checkAndUpdateGeneralRecord(stat, gameStatsList, game, type, recordScope, scopeValue)
                    }
                }
            } else if (recordStatValueService.gameOnlyStats.contains(stat)) {
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
                    seasonRecordService.checkAndUpdateSeasonRecord(
                        stat,
                        homeSeasonGames,
                        awaySeasonGames,
                        type,
                        recordScope,
                        scopeValue,
                        seasonComplete,
                    )
                }
            }
        }
    }

    /**
     * Rate records are held back while a season is live, so a finished season's candidates have to be offered
     * to the board once, when the season closes.
     */
    @Transactional(rollbackFor = [Exception::class])
    fun generateFinishedSeasonRateRecords(seasonNumber: Int) {
        Logger().info("Generating rate records for completed season $seasonNumber")

        val gamesById = gameRepository.findAll().associateBy { it.gameId }
        val seasonGameStats =
            recordEligibleStats(gameStatsRepository.findAll().toList(), gamesById)
                .filter { it.season == seasonNumber }
        val regularSeasonGameStats = seasonGameStats.filter { (it.week ?: 0) < SeasonStructure.POSTSEASON_START_WEEK }
        val postseasonGameStats = seasonGameStats.filter { (it.week ?: 0) >= SeasonStructure.POSTSEASON_START_WEEK }

        val rateStats =
            Stats.values().filter {
                recordQualificationService.isVolumeSensitive(it) &&
                    !recordStatValueService.againstBaseStat.containsKey(it) &&
                    !recordStatValueService.generalRecordStats.contains(it)
            }

        rateStats.forEach { stat ->
            offerFinishedSeason(stat, regularSeasonGameStats, RecordType.SINGLE_SEASON, RecordType.SINGLE_SEASON_LOWEST, true)
            offerFinishedSeason(
                stat,
                postseasonGameStats,
                RecordType.SINGLE_POSTSEASON,
                RecordType.SINGLE_POSTSEASON_LOWEST,
                false,
            )
        }

        Logger().info("Completed generating rate records for season $seasonNumber")
    }

    private fun offerFinishedSeason(
        stat: Stats,
        gameStats: List<GameStats>,
        recordType: RecordType,
        recordTypeLowest: RecordType,
        includeTeamAndConference: Boolean,
    ) {
        gameStats.groupBy { it.team }.forEach { (team, games) ->
            if (team == null) return@forEach
            recordTypesFor(stat, recordType, recordTypeLowest).forEach { type ->
                seasonRecordService.checkAndUpdateSeasonRecord(stat, games, emptyList(), type, RecordScope.LEAGUE, null, true)
                if (!includeTeamAndConference) return@forEach
                seasonRecordService.checkAndUpdateSeasonRecord(stat, games, emptyList(), type, RecordScope.TEAM, team, true)
                conferenceOf(team)?.let { conference ->
                    seasonRecordService.checkAndUpdateSeasonRecord(
                        stat,
                        games,
                        emptyList(),
                        type,
                        RecordScope.CONFERENCE,
                        conference,
                        true,
                    )
                }
            }
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun generateTeamAndConferenceRecords() {
        if (!generationInProgress.compareAndSet(false, true)) {
            throw RecordGenerationInProgressException()
        }
        try {
            Logger().info("Starting generation of team and conference records")
            recordRepository.deleteByRecordScopeIn(RecordScope.TEAM_AND_CONFERENCE.toList())
            val completedGameIds = recordStatValueService.getCompletedGameIds()
            val gamesById = gameRepository.findAll().associateBy { it.gameId }
            val eligibleGameStats = recordEligibleStats(gameStatsRepository.findAll().toList(), gamesById)
            val teamConference = buildTeamConferenceMap(eligibleGameStats.map { it.team })
            val generalGameStats = eligibleGameStats.filter { it.gameId in completedGameIds }

            generateGeneralRecords(generalGameStats, gamesById, teamConference, RecordScope.TEAM_AND_CONFERENCE)
            generateHistoryWideRecords(
                eligibleGameStats,
                completedGameIds,
                teamConference,
                gamesById,
                RecordScope.TEAM_AND_CONFERENCE,
            )

            logRecordCountBreakdown("Completed generation of team and conference records", RecordScope.TEAM_AND_CONFERENCE)
        } finally {
            generationInProgress.set(false)
        }
    }

    private fun generateHistoryWideRecords(
        allGameStats: List<GameStats>,
        completedGameIds: Set<Int>,
        teamConference: Map<String, String?>,
        gamesById: Map<Int, Game>,
        scopes: Set<RecordScope>,
    ) {
        val unfinishedSeason = unfinishedSeason()
        val completeGameStats = allGameStats.filter { it.gameId in completedGameIds }
        val regularSeasonGameStats = allGameStats.filter { (it.week ?: 0) < SeasonStructure.POSTSEASON_START_WEEK }
        val postseasonGameStats = allGameStats.filter { (it.week ?: 0) >= SeasonStructure.POSTSEASON_START_WEEK }
        val completePostseasonGameStats = postseasonGameStats.filter { it.gameId in completedGameIds }
        val generatePostseason = RecordScope.LEAGUE in scopes
        val postseasonScopes = setOf(RecordScope.LEAGUE)

        val statsToProcess =
            Stats.values().filterNot {
                recordStatValueService.againstBaseStat.containsKey(it) || recordStatValueService.generalRecordStats.contains(it)
            }
        statsToProcess.forEachIndexed { index, stat ->
            Logger().info("Generating $stat records (${index + 1}/${statsToProcess.size})")

            if (recordStatValueService.gameOnlyStats.contains(stat)) {
                recordTypesFor(stat, RecordType.SINGLE_GAME, RecordType.SINGLE_GAME_LOWEST).forEach { type ->
                    gameRecordService.generateGameRecord(stat, completeGameStats, type, gamesById, teamConference, scopes)
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
                gameRecordService.generateGameRecord(stat, completeGameStats, type, gamesById, teamConference, scopes)
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
                seasonRecordService.generateSeasonRecord(stat, regularSeasonGameStats, type, teamConference, scopes, unfinishedSeason)
            }
            if (generatePostseason) {
                recordTypesFor(stat, RecordType.SINGLE_POSTSEASON, RecordType.SINGLE_POSTSEASON_LOWEST).forEach { type ->
                    seasonRecordService.generateSeasonRecord(
                        stat,
                        postseasonGameStats,
                        type,
                        teamConference,
                        postseasonScopes,
                        unfinishedSeason,
                    )
                }
            }
        }

        Logger().info("Generating defense (against) records")
        againstRecordService.generateAgainstRecords(
            allGameStats,
            regularSeasonGameStats,
            postseasonGameStats,
            completeGameStats,
            completePostseasonGameStats,
            teamConference,
            scopes,
            unfinishedSeason,
        )
        Logger().info("Completed generating defense (against) records")
    }
}
