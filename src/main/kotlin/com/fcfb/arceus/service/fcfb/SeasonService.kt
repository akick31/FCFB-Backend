package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.model.Season
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.repositories.SeasonRepository
import com.fcfb.arceus.util.CurrentSeasonNotFoundException
import com.fcfb.arceus.util.CurrentWeekNotFoundException
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class SeasonService(
    private val seasonRepository: SeasonRepository,
    private val teamService: TeamService,
    private val userService: UserService,
    private val scheduleRepository: ScheduleRepository,
) {
    /**
     * Start the current season
     */
    fun startSeason(): Season {
        val previousSeason = seasonRepository.getPreviousSeason()
        val season =
            Season(
                seasonNumber = previousSeason?.seasonNumber?.plus(1) ?: 1,
                startDate = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = true,
            )
        teamService.resetWinsAndLosses()
        userService.resetAllDelayOfGameInstances()
        seasonRepository.save(season)
        return season
    }

    /**
     * End the current season
     * @param game the national championship game
     */
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
        season.endDate = now.format(formatter)

        seasonRepository.save(season)
    }

    /**
     * Get the current season
     */
    fun getCurrentSeason() = seasonRepository.getCurrentSeason() ?: throw CurrentSeasonNotFoundException()

    /**
     * Get the current week
     */
    fun getCurrentWeek() = seasonRepository.getCurrentSeason()?.currentWeek ?: throw CurrentWeekNotFoundException()

    /**
     * Increment the current week
     */
    fun incrementWeek() {
        val season = getCurrentSeason()
        season.currentWeek = season.currentWeek.plus(1)
        seasonRepository.save(season)
    }

    /**
     * Get all seasons ordered by season number descending
     */
    fun getAllSeasons(): List<Season> = seasonRepository.getAllSeasons()

    /**
     * Get a specific season by number
     */
    fun getSeasonByNumber(seasonNumber: Int): Season =
        seasonRepository.findBySeasonNumber(seasonNumber)
            ?: throw CurrentSeasonNotFoundException()

    /**
     * Lock the schedule for a given season
     * @param seasonNumber
     */
    fun lockSchedule(seasonNumber: Int): Season {
        val season =
            seasonRepository.findBySeasonNumber(seasonNumber)
                ?: throw CurrentSeasonNotFoundException()
        season.scheduleLocked = true
        seasonRepository.save(season)
        return season
    }

    /**
     * Unlock the schedule for a given season
     * @param seasonNumber
     */
    fun unlockSchedule(seasonNumber: Int): Season {
        val season =
            seasonRepository.findBySeasonNumber(seasonNumber)
                ?: throw CurrentSeasonNotFoundException()
        season.scheduleLocked = false
        seasonRepository.save(season)
        return season
    }

    /**
     * Check if the schedule is locked for a given season
     * @param seasonNumber
     */
    fun isScheduleLocked(seasonNumber: Int): Boolean {
        val season =
            seasonRepository.findBySeasonNumber(seasonNumber)
                ?: return false
        return season.scheduleLocked
    }

    /**
     * Create a season entry for scheduling purposes (does not start the season)
     * Automatically copies bowl games from the previous season if it exists.
     * @param seasonNumber
     */
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

        // Copy bowl games from the previous season (if it exists)
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
                        newGame.week = sourceGame.week // Bowl games are always Week 14
                        newGame.subdivision = sourceGame.subdivision
                        newGame.homeTeam = "TBD" // Teams will be filled in later
                        newGame.awayTeam = "TBD"
                        newGame.tvChannel = sourceGame.tvChannel
                        newGame.gameType = GameType.BOWL
                        newGame.bowlGameName = sourceGame.bowlGameName // Preserve bowl game name
                        newGame.postseasonGameLogo = sourceGame.postseasonGameLogo // Preserve postseason logo
                        newGame.started = false
                        newGame.finished = false
                        newGame
                    }
                scheduleRepository.saveAll(newBowlGames)
                Logger.info("Copied ${newBowlGames.size} bowl games from Season $previousSeason to Season $seasonNumber")
            }
        } catch (e: Exception) {
            // Log but don't fail - it's okay if there are no bowl games to copy
            Logger.warn("Could not copy bowl games from Season $previousSeason: ${e.message}")
        }

        return season
    }
}
