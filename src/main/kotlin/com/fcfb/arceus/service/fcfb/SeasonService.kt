package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Season
import com.fcfb.arceus.repositories.SeasonRepository
import com.fcfb.arceus.util.CurrentSeasonNotFoundException
import com.fcfb.arceus.util.CurrentWeekNotFoundException
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class SeasonService(
    private val seasonRepository: SeasonRepository,
    private val teamService: TeamService,
    private val userService: UserService,
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
}
