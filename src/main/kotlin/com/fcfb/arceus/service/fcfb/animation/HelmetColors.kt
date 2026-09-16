package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import java.awt.Color

object HelmetColors {
    /** The away team switches to its secondary color when the two primaries would be hard to tell apart. */
    fun forMatchup(
        homeTeam: Team,
        awayTeam: Team,
    ): Pair<Color, Color> {
        val home = FieldBackgroundPainter.parseColor(homeTeam.primaryColor)
        val awayPrimary = FieldBackgroundPainter.parseColor(awayTeam.primaryColor)
        val away =
            if (ColorSimilarity.areSimilar(
                    home,
                    awayPrimary,
                )
            ) {
                FieldBackgroundPainter.parseColor(awayTeam.secondaryColor)
            } else {
                awayPrimary
            }
        return home to away
    }
}
