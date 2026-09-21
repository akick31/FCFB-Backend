package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniform
import java.awt.Color

object Uniforms {
    fun forMatchup(
        homeTeam: Team,
        awayTeam: Team,
        homeSnapshot: TeamUniform? = null,
        awaySnapshot: TeamUniform? = null,
    ): Pair<Uniform, Uniform> {
        val (homeHelmet, awayHelmet) = HelmetColors.forMatchup(homeTeam, awayTeam, homeSnapshot, awaySnapshot)
        val home = Uniform(jersey(homeTeam, homeSnapshot), Color.WHITE, homeHelmet, pants(homeTeam, homeSnapshot))
        val away = Uniform(Color.WHITE, jersey(awayTeam, awaySnapshot), awayHelmet, pants(awayTeam, awaySnapshot))
        return home to away
    }

    private fun jersey(
        team: Team,
        snapshot: TeamUniform?,
    ): Color = FieldBackgroundPainter.parseColor(snapshot?.jerseyColor ?: team.primaryColor)

    private fun pants(
        team: Team,
        snapshot: TeamUniform?,
    ): Color = FieldBackgroundPainter.parseColor(snapshot?.pantsColor ?: team.primaryColor)
}
