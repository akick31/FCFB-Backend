package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniform
import com.fcfb.arceus.repositories.TeamUniformRepository
import com.fcfb.arceus.service.fcfb.animation.HelmetColors
import org.springframework.stereotype.Service
import java.awt.Color

@Service
class TeamUniformService(
    private val teamUniformRepository: TeamUniformRepository,
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
    ): TeamUniform? {
        if (season == null || week == null) return null
        return teamUniformRepository.findByTeamAndSeasonNumberAndWeek(team, season, week)
    }

    private fun capture(
        team: String,
        season: Int,
        week: Int,
    ) {
        if (teamUniformRepository.findByTeamAndSeasonNumberAndWeek(team, season, week) != null) return
        teamUniformRepository.save(uniformOf(teamService.getTeamByName(team), season, week))
    }

    /** The helmet color is resolved here rather than at render time because it depends on reading the team's logo. */
    private fun uniformOf(
        team: Team,
        season: Int,
        week: Int,
    ): TeamUniform =
        TeamUniform(
            team = team.name.orEmpty(),
            seasonNumber = season,
            week = week,
            helmetColor = hex(HelmetColors.shellColor(team)),
            facemaskColor = FACEMASK_COLOR,
            jerseyColor = team.primaryColor,
            pantsColor = team.primaryColor,
            logoUrl = team.scorebugLogo,
            hasLogo = true,
            hasStripe = false,
        )

    private fun hex(color: Color): String = String.format("#%06X", color.rgb and 0xFFFFFF)

    companion object {
        private const val FACEMASK_COLOR = "#FFFFFF"
    }
}
