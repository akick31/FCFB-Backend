package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import java.awt.Color

/** Home wears its primary color with white numbers; away wears white with numbers in its primary color. Both wear team pants. */
object Uniforms {
    fun forMatchup(
        homeTeam: Team,
        awayTeam: Team,
    ): Pair<Uniform, Uniform> {
        val (homeHelmet, awayHelmet) = HelmetColors.forMatchup(homeTeam, awayTeam)
        val homePrimary = FieldBackgroundPainter.parseColor(homeTeam.primaryColor)
        val awayPrimary = FieldBackgroundPainter.parseColor(awayTeam.primaryColor)
        val home = Uniform(homePrimary, Color.WHITE, homeHelmet, homePrimary)
        val away = Uniform(Color.WHITE, awayPrimary, awayHelmet, awayPrimary)
        return home to away
    }
}
