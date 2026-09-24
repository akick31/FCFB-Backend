package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniformCurrent
import com.fcfb.arceus.model.TeamUniformHistory
import com.fcfb.arceus.repositories.TeamUniformCurrentRepository
import com.fcfb.arceus.repositories.TeamUniformHistoryRepository
import com.fcfb.arceus.service.fcfb.animation.HelmetColors
import org.springframework.stereotype.Service
import java.awt.Color

@Service
class TeamUniformService(
    private val teamUniformHistoryRepository: TeamUniformHistoryRepository,
    private val teamUniformCurrentRepository: TeamUniformCurrentRepository,
    private val teamService: TeamService,
) {
    /** Captures both teams' uniforms for the week a game is played, so its animations never follow later color changes. */
    fun snapshot(game: Game) {
        val season = game.season ?: return
        val week = game.week ?: return
        listOf(game.homeTeam, game.awayTeam).forEach { capture(it, season, week) }
    }

    fun uniformFor(
        team: String,
        season: Int?,
        week: Int?,
    ): TeamUniformHistory? {
        if (season == null || week == null) return null
        return teamUniformHistoryRepository.findByTeamAndSeasonNumberAndWeek(team, season, week)
    }

    private fun capture(
        team: String,
        season: Int,
        week: Int,
    ) {
        if (teamUniformHistoryRepository.findByTeamAndSeasonNumberAndWeek(team, season, week) != null) return
        teamUniformHistoryRepository.save(uniformOf(teamService.getTeamByName(team), season, week))
    }

    /** The helmet color is resolved here rather than at render time because it depends on reading the team's logo. */
    private fun uniformOf(
        team: Team,
        season: Int,
        week: Int,
    ): TeamUniformHistory {
        val name = team.name.orEmpty()
        val current = teamUniformCurrentRepository.findById(name).orElse(null)
        return TeamUniformHistory(
            team = name,
            seasonNumber = season,
            week = week,
            primaryColor = current?.primaryColor ?: team.primaryColor,
            secondaryColor = current?.secondaryColor ?: team.secondaryColor,
            tertiaryColor = current?.tertiaryColor,
            helmetColor = current?.helmetColor ?: hex(HelmetColors.shellColor(team)),
            secondaryHelmetColor = current?.secondaryHelmetColor,
            helmetNumberColor = current?.helmetNumberColor ?: TeamUniformCurrent.DEFAULT_HELMET_NUMBER_COLOR,
            facemaskColor = current?.facemaskColor ?: FACEMASK_COLOR,
            helmetLogoMode = current?.helmetLogoMode ?: TeamUniformCurrent.DEFAULT_HELMET_LOGO_MODE,
            jerseyColor = current?.jerseyColor ?: team.primaryColor,
            numberColor = current?.numberColor ?: NUMBER_COLOR,
            numberOutlineColor = current?.numberOutlineColor,
            pantsColor = current?.pantsColor ?: team.primaryColor,
            logoUrl = current?.logoUrl ?: team.scorebugLogo,
            hasLogo = current?.hasLogo ?: true,
            hasStripe = current?.hasStripe ?: false,
            stripeColor = current?.stripeColor,
        )
    }

    private fun hex(color: Color): String = String.format("#%06X", color.rgb and 0xFFFFFF)

    companion object {
        private const val FACEMASK_COLOR = "#FFFFFF"
        private const val NUMBER_COLOR = "#FFFFFF"
    }
}
