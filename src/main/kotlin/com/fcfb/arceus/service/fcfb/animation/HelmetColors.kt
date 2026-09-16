package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniform
import java.awt.Color
import java.awt.image.BufferedImage

object HelmetColors {
    private const val PRIMARY_LOGO_SHARE = 0.5f
    private const val SAMPLE_STEP = 2
    private const val OPAQUE_ALPHA = 128

    /** The away team switches to its secondary color when the two primaries would be hard to tell apart. */
    fun forMatchup(
        homeTeam: Team,
        awayTeam: Team,
        homeSnapshot: TeamUniform? = null,
        awaySnapshot: TeamUniform? = null,
    ): Pair<Color, Color> {
        val home = baseShell(homeTeam, homeSnapshot)
        val awayShell = baseShell(awayTeam, awaySnapshot)
        val away = if (ColorSimilarity.areSimilar(home, awayShell)) alternate(awayTeam, awayShell) else awayShell
        return home to away
    }

    /** A stored uniform already holds the resolved shell color, so rendering it never has to read the logo again. */
    private fun baseShell(
        team: Team,
        snapshot: TeamUniform?,
    ): Color = snapshot?.helmetColor?.let { FieldBackgroundPainter.parseColor(it) } ?: shellColor(team)

    /**
     * A logo painted mostly in the team's own color would disappear on the shell, so those teams wear their secondary.
     * Share of the logo is what decides it: outlines and highlights would otherwise veto an obviously monochrome mark.
     */
    fun shellColor(team: Team): Color {
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        val logo = LogoLoader.load(team.scorebugLogo) ?: return primary
        if (similarShare(logo, primary) < PRIMARY_LOGO_SHARE) return primary
        return FieldBackgroundPainter.parseColor(team.secondaryColor)
    }

    private fun similarShare(
        logo: BufferedImage,
        color: Color,
    ): Float {
        var total = 0
        var similar = 0
        for (y in 0 until logo.height step SAMPLE_STEP) {
            for (x in 0 until logo.width step SAMPLE_STEP) {
                val argb = logo.getRGB(x, y)
                if (argb ushr 24 < OPAQUE_ALPHA) continue
                total++
                if (ColorSimilarity.areSimilar(Color(argb), color)) similar++
            }
        }
        return if (total == 0) 0f else similar.toFloat() / total
    }

    private fun alternate(
        team: Team,
        current: Color,
    ): Color {
        val secondary = FieldBackgroundPainter.parseColor(team.secondaryColor)
        if (!ColorSimilarity.areSimilar(current, secondary)) return secondary
        return FieldBackgroundPainter.parseColor(team.primaryColor)
    }
}
