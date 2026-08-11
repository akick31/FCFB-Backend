package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.ConferenceRulesRequest
import com.fcfb.arceus.dto.request.ConferenceScheduleRequest
import com.fcfb.arceus.dto.request.MoveGameRequest
import com.fcfb.arceus.dto.request.ScheduleEntry
import com.fcfb.arceus.dto.response.ConferenceRulesResponse
import com.fcfb.arceus.dto.response.OocGenerationResult
import com.fcfb.arceus.dto.response.ScheduleGenJob
import com.fcfb.arceus.dto.response.ScheduleGenJobResponse
import com.fcfb.arceus.dto.response.ScheduleValidationResult
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.model.Bowl
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.fcfb.schedule.ConferenceRulesService
import com.fcfb.arceus.service.fcfb.schedule.ConferenceScheduleGenerationService
import com.fcfb.arceus.service.fcfb.schedule.OutOfConferenceScheduleGenerationService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleValidationService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.ScheduleNotFoundException
import org.springframework.stereotype.Service

private val NEUTRAL_SITE_GAME_TYPES =
    setOf(GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP, GameType.CONFERENCE_CHAMPIONSHIP, GameType.BOWL)

@Service
open class ScheduleService(
    private val seasonService: SeasonService,
    private val scheduleRepository: ScheduleRepository,
    private val gameRepository: GameRepository,
    private val teamRepository: TeamRepository,
    private val bowlService: BowlService,
    private val venueService: VenueService,
    private val conferenceService: ConferenceService,
    private val conferenceScheduleGenerationService: ConferenceScheduleGenerationService,
    private val conferenceRulesService: ConferenceRulesService,
    private val outOfConferenceScheduleGenerationService: OutOfConferenceScheduleGenerationService,
    private val scheduleValidationService: ScheduleValidationService,
) {
    private fun checkScheduleLock(season: Int) {
        if (seasonService.isScheduleLocked(season)) {
            throw IllegalStateException("Schedule for season $season is locked and cannot be modified")
        }
    }

    private fun resolveNeutralSite(entry: ScheduleEntry): Boolean = entry.gameType in NEUTRAL_SITE_GAME_TYPES || entry.neutralSite

    private fun saveVenueDefault(entry: ScheduleEntry) {
        val venue = entry.venue?.trim()?.takeIf { it.isNotBlank() }
        if (venue != null) venueService.findOrCreateByName(venue)
        when (entry.gameType) {
            GameType.BOWL -> bowlService.upsertPreGame(entry)
            GameType.CONFERENCE_CHAMPIONSHIP -> {
                if (venue == null) return
                val conferenceCode = teamRepository.getTeamByName(entry.homeTeam)?.conference ?: return
                conferenceService.updateChampionshipVenue(conferenceCode, venue)
            }
            else -> {}
        }
    }

    fun getBowl(name: String): Bowl? = bowlService.getByName(name)

    fun getGamesToStartBySeasonAndWeek(
        season: Int,
        week: Int,
    ) = scheduleRepository.getGamesToStartBySeasonAndWeek(season, week)

    fun markGameAsStarted(gameToSchedule: Schedule) {
        gameToSchedule.started = true
        scheduleRepository.save(gameToSchedule)
    }

    fun markManuallyStartedGameAsStarted(game: Game) {
        val gameInSchedule = findGameInSchedule(game)
        gameInSchedule.started = true
        gameInSchedule.gameId = game.gameId
        scheduleRepository.save(gameInSchedule)
    }

    fun markGameAsFinished(game: Game) {
        try {
            val gameInSchedule = findGameInSchedule(game)
            gameInSchedule.finished = true
            gameInSchedule.homeScore = game.homeScore
            gameInSchedule.awayScore = game.awayScore
            scheduleRepository.save(gameInSchedule)
            if (gameInSchedule.gameType == GameType.BOWL) {
                try {
                    game.venue?.trim()?.takeIf { it.isNotBlank() }?.let { venueService.findOrCreateByName(it) }
                    bowlService.recordResult(gameInSchedule, game)
                } catch (e: Exception) {
                    Logger.error("Unable to record bowl result for game ${game.gameId}", e)
                }
            }
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
        val isPostseason = entry.gameType in NEUTRAL_SITE_GAME_TYPES
        if (!isPostseason) {
            checkScheduleLock(entry.season)
        }

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
        schedule.neutralSite = resolveNeutralSite(entry)
        schedule.venue = entry.venue
        val saved = scheduleRepository.save(schedule)
        saveVenueDefault(entry)
        Logger.info("Created schedule entry: ${entry.homeTeam} vs ${entry.awayTeam} (Season ${entry.season}, Week ${entry.week})")
        return saved
    }

    fun createBulkScheduleEntries(entries: List<ScheduleEntry>): List<Schedule> {
        entries.firstOrNull()?.let { checkScheduleLock(it.season) }

        val placeholderTeams = setOf("TBD", "OPEN", "")

        val teamWeekPairs = mutableSetOf<String>()
        val validEntries = mutableListOf<ScheduleEntry>()

        for (entry in entries) {
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
                schedule.neutralSite = resolveNeutralSite(entry)
                schedule.venue = entry.venue
                schedule
            }
        val saved = scheduleRepository.saveAll(schedules).toList()
        validEntries.forEach { saveVenueDefault(it) }
        Logger.info("Created ${saved.size} schedule entries in bulk for season ${entries.firstOrNull()?.season}")
        return saved
    }

    fun updateScheduleEntry(
        id: Int,
        entry: ScheduleEntry,
    ): Schedule {
        val isPostseason = entry.gameType in NEUTRAL_SITE_GAME_TYPES
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
        schedule.neutralSite = resolveNeutralSite(entry)
        schedule.venue = entry.venue
        val saved = scheduleRepository.save(schedule)
        saveVenueDefault(entry)
        Logger.info("Updated schedule entry $id: ${entry.homeTeam} vs ${entry.awayTeam}")
        return saved
    }

    fun deleteScheduleEntry(id: Int) {
        val existing = scheduleRepository.findById(id).orElse(null)
        if (existing != null) {
            val isPostseason = existing.gameType in NEUTRAL_SITE_GAME_TYPES
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

    fun getPostseasonSchedule(season: Int): List<Schedule> {
        val entries = scheduleRepository.getPostseasonSchedule(season) ?: emptyList()
        if (entries.isEmpty()) return entries
        val games = gameRepository.getGamesBySeason(season)
        return enrichScheduleWithGameData(entries, games)
    }

    private fun enrichScheduleWithGameData(
        entries: List<Schedule>,
        games: List<Game>,
    ): List<Schedule> {
        if (games.isEmpty()) return entries

        val gameById = games.associateBy { it.gameId }

        val gameByMatchup = mutableMapOf<String, Game>()
        for (game in games) {
            val s = game.season ?: 0
            val w = game.week ?: 0
            gameByMatchup[matchupKey(game.homeTeam, game.awayTeam, s, w)] = game
            gameByMatchup[matchupKey(game.awayTeam, game.homeTeam, s, w)] = game
        }

        for (entry in entries) {
            val key = matchupKey(entry.homeTeam, entry.awayTeam, entry.season, entry.week)

            val game =
                if (entry.gameId != null) {
                    gameById[entry.gameId] ?: gameByMatchup[key]
                } else {
                    gameByMatchup[key]
                }

            if (game != null) {
                if (game.homeTeam == entry.homeTeam) {
                    entry.homeScore = game.homeScore
                    entry.awayScore = game.awayScore
                } else {
                    entry.homeScore = game.awayScore
                    entry.awayScore = game.homeScore
                }

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

    fun generateConferenceSchedule(request: ConferenceScheduleRequest): List<Schedule> =
        conferenceScheduleGenerationService.generateConferenceSchedule(request)

    fun startAllConferenceGenerationAsync(season: Int): ScheduleGenJobResponse =
        conferenceScheduleGenerationService.startAllConferenceGenerationAsync(season)

    fun getScheduleGenJobStatus(jobId: String): ScheduleGenJob =
        conferenceScheduleGenerationService.getScheduleGenJobStatus(jobId)
            ?: throw ScheduleNotFoundException("No schedule generation job found with id $jobId")

    fun generateOutOfConferenceSchedule(season: Int): OocGenerationResult =
        outOfConferenceScheduleGenerationService.generateOutOfConferenceSchedule(season)

    fun saveConferenceRules(request: ConferenceRulesRequest): ConferenceRulesResponse = conferenceRulesService.saveConferenceRules(request)

    fun getConferenceRules(conference: String): ConferenceRulesResponse =
        conferenceRulesService.getConferenceRules(conference)
            ?: throw ScheduleNotFoundException("No conference rules found for $conference")

    fun validateSchedule(season: Int): ScheduleValidationResult = scheduleValidationService.validateSchedule(season)
}
