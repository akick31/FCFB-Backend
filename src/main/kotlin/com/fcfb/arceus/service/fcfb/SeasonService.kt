package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.model.Season
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.repositories.SeasonRepository
import com.fcfb.arceus.util.CurrentSeasonNotFoundException
import com.fcfb.arceus.util.CurrentWeekNotFoundException
import com.fcfb.arceus.util.Logger
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
) {
    fun startSeason(): Season {
        val now = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        val pendingSeason = seasonRepository.getPendingSeason()
        val season =
            if (pendingSeason != null) {
                pendingSeason.startDate = now
                pendingSeason.currentSeason = true
                pendingSeason
            } else {
                val previousSeason = seasonRepository.getPreviousSeason()
                Season(
                    seasonNumber = previousSeason?.seasonNumber?.plus(1) ?: 1,
                    startDate = now,
                    endDate = null,
                    nationalChampionshipWinningTeam = null,
                    nationalChampionshipLosingTeam = null,
                    nationalChampionshipWinningCoach = null,
                    nationalChampionshipLosingCoach = null,
                    currentWeek = 1,
                    currentSeason = true,
                )
            }
        teamService.resetWinsAndLosses()
        userService.resetAllDelayOfGameInstances()
        seasonRepository.save(season)
        offseasonService.endOffseason(now)
        return season
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
