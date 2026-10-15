package com.fcfb.arceus.rendering.chart

import com.fcfb.arceus.rendering.animation.appearance.ColorSimilarity
import java.awt.Color
import kotlin.math.pow

internal class ChartColor {
    fun select(
        primary: Color?,
        secondary: Color?,
        background: Color,
    ): Color = listOfNotNull(primary, secondary).firstOrNull { contrast(it, background) >= MIN_CONTRAST } ?: Color.WHITE

    /**
     * Resolves both teams' colors together so a matchup never renders two indistinguishable
     * lines: a clash sends the away team to its secondary color, then the home team to its
     * secondary too if that still doesn't read against the background, and finally forces the
     * away team to plain white if neither secondary reads against the background.
     */
    fun selectPair(
        homePrimary: Color?,
        homeSecondary: Color?,
        awayPrimary: Color?,
        awaySecondary: Color?,
        background: Color,
    ): Pair<Color, Color> {
        val home = select(homePrimary, homeSecondary, background)
        val away = select(awayPrimary, awaySecondary, background)

        if (!ColorSimilarity.areSimilar(home, away)) {
            return home to away
        }

        val awayAlternate = awaySecondary ?: Color.WHITE
        if (contrast(awayAlternate, background) >= MIN_CONTRAST) {
            return home to awayAlternate
        }

        val homeAlternate = homeSecondary ?: Color.WHITE
        if (contrast(homeAlternate, background) < MIN_CONTRAST) {
            return home to Color.WHITE
        }
        return homeAlternate to awayAlternate
    }

    private fun contrast(
        color: Color,
        background: Color,
    ): Double {
        val foregroundLuminance = luminance(color)
        val backgroundLuminance = luminance(background)
        return (maxOf(foregroundLuminance, backgroundLuminance) + 0.05) /
            (minOf(foregroundLuminance, backgroundLuminance) + 0.05)
    }

    private fun luminance(color: Color): Double =
        0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)

    private fun linearize(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private companion object {
        const val MIN_CONTRAST = 3.0
    }
}
