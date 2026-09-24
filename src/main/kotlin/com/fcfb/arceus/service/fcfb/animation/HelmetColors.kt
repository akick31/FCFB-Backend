package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniformHistory
import java.awt.Color
import java.awt.image.BufferedImage

object HelmetColors {
    private const val PRIMARY_LOGO_SHARE = 0.5f
    private const val SAMPLE_STEP = 2
    private const val OPAQUE_ALPHA = 128
    private const val LIGHT_LUMINANCE = 128

    fun forMatchup(
        homeTeam: Team,
        awayTeam: Team,
        homeSnapshot: TeamUniformHistory? = null,
        awaySnapshot: TeamUniformHistory? = null,
    ): Pair<Color, Color> {
        val home = baseShell(homeTeam, homeSnapshot)
        val awayShell = baseShell(awayTeam, awaySnapshot)
        val away = if (ColorSimilarity.areSimilar(home, awayShell)) alternateShell(awayTeam, awaySnapshot, awayShell) else awayShell
        return home to away
    }

    private fun baseShell(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = snapshot?.helmetColor?.let { FieldBackgroundPainter.parseColor(it) } ?: shellColor(team)

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

    /** The team's own alternate shell wins when it is set and actually contrasts; otherwise fall back to the team colors. */
    private fun alternateShell(
        team: Team,
        snapshot: TeamUniformHistory?,
        current: Color,
    ): Color {
        val chosen =
            snapshot?.secondaryHelmetColor
                ?.let { FieldBackgroundPainter.parseColor(it) }
                ?.takeIf { !ColorSimilarity.areSimilar(it, current) }
        return chosen ?: alternate(team, current)
    }

    private fun alternate(
        team: Team,
        current: Color,
    ): Color {
        val secondary = FieldBackgroundPainter.parseColor(team.secondaryColor)
        if (!ColorSimilarity.areSimilar(current, secondary)) return secondary
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        if (!ColorSimilarity.areSimilar(current, primary)) return primary
        return contrastTo(current)
    }

    /** Both team colors clash with the opponent, so neither can separate the sides. Fall back to plain contrast. */
    private fun contrastTo(color: Color): Color {
        val luminance = (color.red * 299 + color.green * 587 + color.blue * 114) / 1000
        return if (luminance > LIGHT_LUMINANCE) Color.BLACK else Color.WHITE
    }
}
