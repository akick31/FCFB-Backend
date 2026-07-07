package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.ConferenceRulesRequest
import com.fcfb.arceus.dto.ConferenceRulesResponse
import com.fcfb.arceus.dto.ConferenceScheduleRequest
import com.fcfb.arceus.dto.MoveGameRequest
import com.fcfb.arceus.dto.ScheduleEntry
import com.fcfb.arceus.dto.ScheduleGenJob
import com.fcfb.arceus.dto.ScheduleGenJobResponse
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.service.fcfb.schedule.ConferenceRulesService
import com.fcfb.arceus.service.fcfb.schedule.ConferenceScheduleGenerationService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.ScheduleNotFoundException
import org.springframework.stereotype.Service

@Service
open class ScheduleService(
    private val seasonService: SeasonService,
    private val scheduleRepository: ScheduleRepository,
    private val gameRepository: GameRepository,
    private val conferenceScheduleGenerationService: ConferenceScheduleGenerationService,
    private val conferenceRulesService: ConferenceRulesService,
) {
    /**
     * Check if the schedule is locked for a given season and throw if locked
     * @param season
     */
    private fun checkScheduleLock(season: Int) {
        if (seasonService.isScheduleLocked(season)) {
            throw IllegalStateException("Schedule for season $season is locked and cannot be modified")
        }
    }

    fun getGamesToStartBySeasonAndWeek(
        season: Int,
        week: Int,
    ) = scheduleRepository.getGamesToStartBySeasonAndWeek(season, week)

    fun markGameAsStarted(gameToSchedule: Schedule) {
        gameToSchedule.started = true
        scheduleRepository.save(gameToSchedule)
    }

    /**
     * Mark a manually started game as started and store the game_id
     * @param game
     */
    fun markManuallyStartedGameAsStarted(game: Game) {
        val gameInSchedule = findGameInSchedule(game)
        gameInSchedule.started = true
        gameInSchedule.gameId = game.gameId
        scheduleRepository.save(gameInSchedule)
    }

    /**
     * Mark a game as finished and store the final scores
     * @param game
     */
    fun markGameAsFinished(game: Game) {
        try {
            val gameInSchedule = findGameInSchedule(game)
            gameInSchedule.finished = true
            gameInSchedule.homeScore = game.homeScore
            gameInSchedule.awayScore = game.awayScore
            scheduleRepository.save(gameInSchedule)
            if (checkIfWeekIsOver(game.season ?: 0, game.week ?: 0)) {
                seasonService.incrementWeek()
            }
        } catch (e: Exception) {
            Logger.error("Unable to mark game as finished", e)
        }
    }

    private fun checkIfWeekIsOver(
        season: Int,
        week: Int,
    ) = scheduleRepository.checkIfWeekIsOver(season, week) == 1

    private fun findGameInSchedule(game: Game) =
        scheduleRepository.findGameInSchedule(
            game.homeTeam,
            game.awayTeam,
            game.season ?: 0,
            game.week ?: 0,
        ) ?: throw ScheduleNotFoundException("Game not found in schedule")

    fun getTeamOpponent(team: String) =
        scheduleRepository.getTeamOpponent(
            seasonService.getCurrentSeason().seasonNumber,
            seasonService.getCurrentWeek(),
            team,
        ) ?: throw ScheduleNotFoundException("Opponent not found for $team")

    /**
     * Get the schedule for a given season for a team, enriched with game data (scores + game_id)
     * @param season
     * @param team
     */
    fun getScheduleBySeasonAndTeam(
        season: Int,
        team: String,
    ): List<Schedule> {
        val entries =
            scheduleRepository.getScheduleBySeasonAndTeam(season, team)
                ?: throw ScheduleNotFoundException("Schedule not found for $team")
        val games = gameRepository.getGamesBySeasonAndTeam(season, team)
        return enrichScheduleWithGameData(entries, games)
    }

    fun getScheduleBySeason(season: Int) =
        scheduleRepository.getScheduleBySeason(season)
            ?: throw ScheduleNotFoundException("Schedule not found for season $season")

    fun getScheduleBySeasonAndWeek(
        season: Int,
        week: Int,
    ) = scheduleRepository.getScheduleBySeasonAndWeek(season, week)
        ?: throw ScheduleNotFoundException("Schedule not found for season $season week $week")

    /**
     * Get the conference schedule, enriched with game data
     * @param season
     * @param conference
     */
    fun getConferenceSchedule(
        season: Int,
        conference: String,
    ): List<Schedule> {
        val entries =
            scheduleRepository.getConferenceSchedule(season, conference)
                ?: throw ScheduleNotFoundException("Conference schedule not found for $conference")
        val games = gameRepository.getGamesBySeason(season)
        return enrichScheduleWithGameData(entries, games)
    }

    fun createScheduleEntry(entry: ScheduleEntry): Schedule {
        // Postseason entries (PLAYOFFS, NATIONAL_CHAMPIONSHIP, CONFERENCE_CHAMPIONSHIP, BOWL) bypass schedule lock
        val isPostseason =
            entry.gameType in
                listOf(
                    GameType.PLAYOFFS,
                    GameType.NATIONAL_CHAMPIONSHIP,
                    GameType.CONFERENCE_CHAMPIONSHIP,
                    GameType.BOWL,
                )
        if (!isPostseason) {
            checkScheduleLock(entry.season)
        }

        // Prevent scheduling a team twice in the same week (skip placeholder teams)
        val placeholderTeams = setOf("TBD", "OPEN", "")
        if (entry.homeTeam !in placeholderTeams) {
            if (isTeamScheduledInWeek(entry.season, entry.week, entry.homeTeam)) {
                throw IllegalStateException(
                    "${entry.homeTeam} already has a game scheduled in Season ${entry.season}, Week ${entry.week}",
                )
            }
        }
        if (entry.awayTeam !in placeholderTeams) {
            if (isTeamScheduledInWeek(entry.season, entry.week, entry.awayTeam)) {
                throw IllegalStateException(
                    "${entry.awayTeam} already has a game scheduled in Season ${entry.season}, Week ${entry.week}",
                )
            }
        }

        val schedule = Schedule()
        schedule.season = entry.season
        schedule.week = entry.week
        schedule.subdivision = entry.subdivision
        schedule.homeTeam = entry.homeTeam
        schedule.awayTeam = entry.awayTeam
        schedule.tvChannel =
            if (
                entry.gameType in
                listOf(
                    GameType.BOWL,
                    GameType.PLAYOFFS,
                    GameType.NATIONAL_CHAMPIONSHIP,
                )
            ) {
                TVChannel.ESPN
            } else {
                entry.tvChannel
            }
        schedule.gameType = entry.gameType
        schedule.started = false
        schedule.finished = false
        schedule.playoffRound = entry.playoffRound
        schedule.playoffHomeSeed = entry.playoffHomeSeed
        schedule.playoffAwaySeed = entry.playoffAwaySeed
        schedule.postseasonGameName = entry.postseasonGameName
        schedule.postseasonGameLogo = entry.postseasonGameLogo
        val saved = scheduleRepository.save(schedule)
        Logger.info("Created schedule entry: ${entry.homeTeam} vs ${entry.awayTeam} (Season ${entry.season}, Week ${entry.week})")
        return saved
    }

    fun createBulkScheduleEntries(entries: List<ScheduleEntry>): List<Schedule> {
        entries.firstOrNull()?.let { checkScheduleLock(it.season) }

        val placeholderTeams = setOf("TBD", "OPEN", "")

        // Validate: no team should appear twice in the same week within the batch
        val teamWeekPairs = mutableSetOf<String>()
        val validEntries = mutableListOf<ScheduleEntry>()

        for (entry in entries) {
            // Skip self-matchups (a team playing itself — should never happen but guards against bugs)
            if (entry.homeTeam == entry.awayTeam && entry.homeTeam !in placeholderTeams) {
                Logger.warn("Skipping self-matchup: ${entry.homeTeam} vs ${entry.awayTeam} in S${entry.season} W${entry.week}")
                continue
            }

            val homeKey = "${entry.homeTeam}|${entry.season}|${entry.week}"
            val awayKey = "${entry.awayTeam}|${entry.season}|${entry.week}"
            var conflict = false

            if (entry.homeTeam !in placeholderTeams && homeKey in teamWeekPairs) {
                Logger.warn("Skipping duplicate: ${entry.homeTeam} already scheduled in S${entry.season} W${entry.week}")
                conflict = true
            }
            if (!conflict && entry.awayTeam !in placeholderTeams && awayKey in teamWeekPairs) {
                Logger.warn("Skipping duplicate: ${entry.awayTeam} already scheduled in S${entry.season} W${entry.week}")
                conflict = true
            }

            if (!conflict) {
                if (entry.homeTeam !in placeholderTeams) teamWeekPairs.add(homeKey)
                if (entry.awayTeam !in placeholderTeams) teamWeekPairs.add(awayKey)
                validEntries.add(entry)
            }
        }

        if (validEntries.size < entries.size) {
            Logger.warn("Filtered out ${entries.size - validEntries.size} duplicate entries from bulk create")
        }

        val schedules =
            validEntries.map { entry ->
                val schedule = Schedule()
                schedule.season = entry.season
                schedule.week = entry.week
                schedule.subdivision = entry.subdivision
                schedule.homeTeam = entry.homeTeam
                schedule.awayTeam = entry.awayTeam
                schedule.tvChannel =
                    if (
                        entry.gameType in listOf(GameType.BOWL, GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP)
                    ) {
                        TVChannel.ESPN
                    } else {
                        entry.tvChannel
                    }
                schedule.gameType = entry.gameType
                schedule.started = false
                schedule.finished = false
                schedule.playoffRound = entry.playoffRound
                schedule.playoffHomeSeed = entry.playoffHomeSeed
                schedule.playoffAwaySeed = entry.playoffAwaySeed
                schedule.postseasonGameName = entry.postseasonGameName
                schedule.postseasonGameLogo = entry.postseasonGameLogo
                schedule
            }
        val saved = scheduleRepository.saveAll(schedules).toList()
        Logger.info("Created ${saved.size} schedule entries in bulk for season ${entries.firstOrNull()?.season}")
        return saved
    }

    fun updateScheduleEntry(
        id: Int,
        entry: ScheduleEntry,
    ): Schedule {
        // Postseason entries bypass schedule lock
        val isPostseason =
            entry.gameType in
                listOf(
                    GameType.PLAYOFFS,
                    GameType.NATIONAL_CHAMPIONSHIP,
                    GameType.CONFERENCE_CHAMPIONSHIP,
                    GameType.BOWL,
                )
        if (!isPostseason) {
            checkScheduleLock(entry.season)
        }
        val schedule =
            scheduleRepository.findById(id).orElseThrow {
                ScheduleNotFoundException("Schedule entry not found with id $id")
            }
        schedule.season = entry.season
        schedule.week = entry.week
        schedule.subdivision = entry.subdivision
        schedule.homeTeam = entry.homeTeam
        schedule.awayTeam = entry.awayTeam
        schedule.tvChannel =
            if (entry.gameType in
                listOf(
                    GameType.BOWL, GameType.PLAYOFFS,
                    GameType.NATIONAL_CHAMPIONSHIP,
                )
            ) {
                TVChannel.ESPN
            } else {
                entry.tvChannel
            }
        schedule.gameType = entry.gameType
        schedule.playoffRound = entry.playoffRound
        schedule.playoffHomeSeed = entry.playoffHomeSeed
        schedule.playoffAwaySeed = entry.playoffAwaySeed
        schedule.postseasonGameName = entry.postseasonGameName
        schedule.postseasonGameLogo = entry.postseasonGameLogo
        val saved = scheduleRepository.save(schedule)
        Logger.info("Updated schedule entry $id: ${entry.homeTeam} vs ${entry.awayTeam}")
        return saved
    }

    fun deleteScheduleEntry(id: Int) {
        val existing = scheduleRepository.findById(id).orElse(null)
        if (existing != null) {
            // Postseason entries bypass schedule lock
            val isPostseason =
                existing.gameType in
                    listOf(
                        GameType.PLAYOFFS,
                        GameType.NATIONAL_CHAMPIONSHIP,
                        GameType.CONFERENCE_CHAMPIONSHIP,
                        GameType.BOWL,
                    )
            if (!isPostseason) {
                checkScheduleLock(existing.season)
            }
        }
        scheduleRepository.deleteById(id)
        Logger.info("Deleted schedule entry $id")
    }

    fun deleteScheduleBySeason(season: Int) {
        checkScheduleLock(season)
        scheduleRepository.deleteScheduleBySeason(season)
        Logger.info("Deleted all schedule entries for season $season")
    }

    fun moveGame(request: MoveGameRequest): Schedule {
        val schedule =
            scheduleRepository.findById(request.scheduleId).orElseThrow {
                ScheduleNotFoundException("Schedule entry not found with id ${request.scheduleId}")
            }
        checkScheduleLock(schedule.season)
        val oldWeek = schedule.week
        schedule.week = request.newWeek
        val saved = scheduleRepository.save(schedule)
        Logger.info("Moved game ${schedule.homeTeam} vs ${schedule.awayTeam} from week $oldWeek to week ${request.newWeek}")
        return saved
    }

    fun getScheduleById(id: Int): Schedule =
        scheduleRepository.findById(id).orElseThrow {
            ScheduleNotFoundException("Schedule entry not found with id $id")
        }

    fun getScheduleBySeasonWeekAndTeam(
        season: Int,
        week: Int,
        team: String,
    ): Schedule? = scheduleRepository.getScheduleBySeasonWeekAndTeam(season, week, team)

    /**
     * Get the postseason schedule (playoffs, bowls, conference championships, national championship),
     * enriched with game data
     * @param season
     */
    fun getPostseasonSchedule(season: Int): List<Schedule> {
        val entries = scheduleRepository.getPostseasonSchedule(season) ?: emptyList()
        if (entries.isEmpty()) return entries
        val games = gameRepository.getGamesBySeason(season)
        return enrichScheduleWithGameData(entries, games)
    }

    /**
     * Enrich schedule entries with game data (scores and game_id) from the game table.
     *
     * For each schedule entry, find the matching game by:
     *   1. game_id (if already stored in schedule)
     *   2. Fallback: matching on (home_team, away_team, season, week)
     *      — also checks the reverse matchup (away_team as home, home_team as away)
     *      — week is always included to handle rematches in the same season
     *
     * Populates homeScore, awayScore, and gameId on the schedule entry.
     * Scores are mapped correctly even when home/away are swapped between
     * the schedule and game tables.
     * Also lazy-backfills game_id into the schedule table for future fast lookups.
     */
    private fun enrichScheduleWithGameData(
        entries: List<Schedule>,
        games: List<Game>,
    ): List<Schedule> {
        if (games.isEmpty()) return entries

        // Build lookup maps for fast matching
        val gameById = games.associateBy { it.gameId }

        // Index games by both forward AND reverse matchup keys so we can find a
        // game even when the home/away sides are swapped between schedule and game table.
        val gameByMatchup = mutableMapOf<String, Game>()
        for (game in games) {
            val s = game.season ?: 0
            val w = game.week ?: 0
            gameByMatchup[matchupKey(game.homeTeam, game.awayTeam, s, w)] = game
            gameByMatchup[matchupKey(game.awayTeam, game.homeTeam, s, w)] = game
        }

        for (entry in entries) {
            val key = matchupKey(entry.homeTeam, entry.awayTeam, entry.season, entry.week)

            // Find the matching game — prefer by gameId, then fall back to matchup key
            val game =
                if (entry.gameId != null) {
                    gameById[entry.gameId] ?: gameByMatchup[key]
                } else {
                    gameByMatchup[key]
                }

            if (game != null) {
                // Map scores correctly: schedule homeTeam might not match game homeTeam
                if (game.homeTeam == entry.homeTeam) {
                    entry.homeScore = game.homeScore
                    entry.awayScore = game.awayScore
                } else {
                    // Teams are swapped between schedule and game — flip scores
                    entry.homeScore = game.awayScore
                    entry.awayScore = game.homeScore
                }

                // Lazy-backfill game_id if missing
                if (entry.gameId == null) {
                    entry.gameId = game.gameId
                    try {
                        scheduleRepository.save(entry)
                    } catch (e: Exception) {
                        Logger.warn("Could not backfill game_id for schedule entry ${entry.id}: ${e.message}")
                    }
                }
            }
        }

        return entries
    }

    private fun matchupKey(
        homeTeam: String,
        awayTeam: String,
        season: Int,
        week: Int,
    ): String = "$homeTeam|$awayTeam|$season|$week"

    fun isTeamScheduledInWeek(
        season: Int,
        week: Int,
        team: String,
    ): Boolean = scheduleRepository.getScheduleBySeasonWeekAndTeam(season, week, team) != null

    fun generateConferenceSchedule(
        request: ConferenceScheduleRequest,
        conferenceTeams: List<Team>,
    ): List<Schedule> = conferenceScheduleGenerationService.generateConferenceSchedule(request, conferenceTeams)

    fun startAllConferenceGenerationAsync(season: Int): ScheduleGenJobResponse =
        conferenceScheduleGenerationService.startAllConferenceGenerationAsync(season)

    fun getScheduleGenJobStatus(jobId: String): ScheduleGenJob? = conferenceScheduleGenerationService.getScheduleGenJobStatus(jobId)

    fun saveConferenceRules(request: ConferenceRulesRequest): ConferenceRulesResponse = conferenceRulesService.saveConferenceRules(request)

    fun getConferenceRules(conference: Conference): ConferenceRulesResponse? = conferenceRulesService.getConferenceRules(conference)
}
