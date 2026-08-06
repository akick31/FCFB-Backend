package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.model.Season
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.repositories.SeasonRepository
import com.fcfb.arceus.service.fcfb.schedule.ScheduleValidationService
import com.fcfb.arceus.util.CurrentSeasonNotFoundException
import com.fcfb.arceus.util.CurrentWeekNotFoundException
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.SeasonNotReadyException
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class SeasonService(
    private val seasonRepository: SeasonRepository,
    private val offseasonService: OffseasonService,
    private val teamService: TeamService,
    private val userService: UserService,
    private val scheduleRepository: ScheduleRepository,
    private val teamSeasonConferenceService: TeamSeasonConferenceService,
    private val scheduleValidationService: ScheduleValidationService,
) {
    fun startSeason(): Season {
        val pendingSeason =
            seasonRepository.getPendingSeason()
                ?: throw SeasonNotReadyException("No pending season to start. Create a season for scheduling first.")

        if (!pendingSeason.scheduleLocked) {
            throw SeasonNotReadyException(
                "Cannot start Season ${pendingSeason.seasonNumber}: the schedule must be locked before starting.",
            )
        }

        val validation = scheduleValidationService.validateSchedule(pendingSeason.seasonNumber)
        if (!validation.valid) {
            val details =
                validation.incompleteTeams.joinToString("; ") {
                    "${it.team} (missing weeks ${it.missingWeeks.joinToString(", ")})"
                }
            throw SeasonNotReadyException(
                "Cannot start Season ${pendingSeason.seasonNumber}: schedule is incomplete. $details",
            )
        }

        val now = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        pendingSeason.startDate = now
        pendingSeason.currentSeason = true
        teamService.resetWinsAndLosses()
        userService.resetAllDelayOfGameInstances()
        seasonRepository.save(pendingSeason)
        teamSeasonConferenceService.snapshotSeason(pendingSeason.seasonNumber)
        offseasonService.endOffseason(now)
        return pendingSeason
    }

    fun endSeason(game: Game) {
        val season = getCurrentSeason()
        season.currentSeason = false

        if (game.homeScore > game.awayScore) {
            season.nationalChampionshipWinningTeam = game.homeTeam
            season.nationalChampionshipLosingTeam = game.awayTeam
            season.nationalChampionshipWinningCoach = game.homeCoaches?.joinToString(",")
            season.nationalChampionshipLosingCoach = game.awayCoaches?.joinToString(",")
        } else {
            season.nationalChampionshipWinningTeam = game.awayTeam
            season.nationalChampionshipLosingTeam = game.homeTeam
            season.nationalChampionshipWinningCoach = game.awayCoaches?.joinToString(",")
            season.nationalChampionshipLosingCoach = game.homeCoaches?.joinToString(",")
        }

        val now = ZonedDateTime.now(ZoneId.of("America/New_York"))
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
        val nowFormatted = now.format(formatter)
        season.endDate = nowFormatted

        seasonRepository.save(season)
        offseasonService.startOffseason(nowFormatted)
    }

    fun endSeasonManually(): Season {
        val season = getCurrentSeason()
        if (!hasFinishedNationalChampionship(season.seasonNumber)) {
            throw SeasonNotReadyException(
                "Cannot end Season ${season.seasonNumber}: no finished National Championship game found.",
            )
        }
        season.currentSeason = false
        val now = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        season.endDate = now
        seasonRepository.save(season)
        offseasonService.startOffseason(now)
        return season
    }

    fun hasFinishedNationalChampionship(seasonNumber: Int): Boolean =
        (scheduleRepository.getPostseasonSchedule(seasonNumber) ?: emptyList())
            .any { it.gameType == GameType.NATIONAL_CHAMPIONSHIP && it.finished == true }

    fun setCurrentSeason(seasonNumber: Int): Season {
        val target =
            seasonRepository.findBySeasonNumber(seasonNumber)
                ?: throw CurrentSeasonNotFoundException()
        seasonRepository.findByCurrentSeason(true)
            .filter { it.seasonNumber != seasonNumber }
            .forEach {
                it.currentSeason = false
                seasonRepository.save(it)
            }
        target.currentSeason = true
        seasonRepository.save(target)
        return target
    }

    fun updateCurrentWeek(
        seasonNumber: Int,
        week: Int,
    ): Season {
        val season =
            seasonRepository.findBySeasonNumber(seasonNumber)
                ?: throw CurrentSeasonNotFoundException()
        season.currentWeek = week
        seasonRepository.save(season)
        return season
    }

    fun getCurrentSeason() = seasonRepository.getCurrentSeason() ?: throw CurrentSeasonNotFoundException()

    fun getCurrentWeek() = seasonRepository.getCurrentSeason()?.currentWeek ?: throw CurrentWeekNotFoundException()

    fun getUpcomingSeason(): Season? = seasonRepository.getPendingSeason()

    fun getLatestCompletedSeason(): Season? = seasonRepository.getMostRecentlyCompletedSeason()

    fun incrementWeek() {
        val season = getCurrentSeason()
        season.currentWeek = season.currentWeek.plus(1)
        seasonRepository.save(season)
    }

    fun getAllSeasons(): List<Season> = seasonRepository.getAllSeasons()

    fun getSeasonByNumber(seasonNumber: Int): Season =
        seasonRepository.findBySeasonNumber(seasonNumber)
            ?: throw CurrentSeasonNotFoundException()

    fun lockSchedule(seasonNumber: Int): Season {
        val season =
            seasonRepository.findBySeasonNumber(seasonNumber)
                ?: throw CurrentSeasonNotFoundException()
        season.scheduleLocked = true
        seasonRepository.save(season)
        return season
    }

    fun unlockSchedule(seasonNumber: Int): Season {
        val season =
            seasonRepository.findBySeasonNumber(seasonNumber)
                ?: throw CurrentSeasonNotFoundException()
        season.scheduleLocked = false
        seasonRepository.save(season)
        return season
    }

    fun isScheduleLocked(seasonNumber: Int): Boolean {
        val season =
            seasonRepository.findBySeasonNumber(seasonNumber)
                ?: return false
        return season.scheduleLocked
    }

    fun createSeasonForScheduling(seasonNumber: Int): Season {
        val existing = seasonRepository.findBySeasonNumber(seasonNumber)
        if (existing != null) {
            throw IllegalStateException("Season $seasonNumber already exists")
        }
        val season =
            Season(
                seasonNumber = seasonNumber,
                startDate = null,
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = false,
            )
        seasonRepository.save(season)

        val previousSeason = seasonNumber - 1
        try {
            val sourceBowlGames =
                scheduleRepository.getScheduleBySeason(previousSeason)
                    ?.filter { it.gameType == GameType.BOWL } ?: emptyList()

            if (sourceBowlGames.isNotEmpty()) {
                val newBowlGames =
                    sourceBowlGames.map { sourceGame ->
                        val newGame = Schedule()
                        newGame.season = seasonNumber
                        newGame.week = sourceGame.week
                        newGame.subdivision = sourceGame.subdivision
                        newGame.homeTeam = "TBD"
                        newGame.awayTeam = "TBD"
                        newGame.tvChannel = TVChannel.ESPN
                        newGame.gameType = GameType.BOWL
                        newGame.postseasonGameName = sourceGame.postseasonGameName
                        newGame.postseasonGameLogo = sourceGame.postseasonGameLogo
                        newGame.started = false
                        newGame.finished = false
                        newGame
                    }
                scheduleRepository.saveAll(newBowlGames)
                Logger.info("Copied ${newBowlGames.size} bowl games from Season $previousSeason to Season $seasonNumber")
            }
        } catch (e: Exception) {
            Logger.warn("Could not copy bowl games from Season $previousSeason: ${e.message}")
        }

        return season
    }
}
