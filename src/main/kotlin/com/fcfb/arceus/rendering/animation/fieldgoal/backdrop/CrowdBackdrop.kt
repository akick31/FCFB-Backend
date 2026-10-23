package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.rendering.animation.appearance.ColorSimilarity
import com.fcfb.arceus.rendering.animation.appearance.FieldColors
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object CrowdBackdrop {
    private const val SEED = 42L
    private const val FAN_DOT_SIZE = 16
    private const val FAN_COL_SPACING = 30
    private const val TIER_HEIGHT = 35
    private const val MIN_FAN_SIZE = 3
    private const val MIN_FAN_SPACING = 8
    private const val MIN_TIER_HEIGHT = 8
    private const val HOME_PRIMARY_SHARE = 8
    private const val HOME_SECONDARY_SHARE = 2
    private const val HOME_VISITOR_SHARE = 1
    private const val TIER_CONTRAST_MIDPOINT = 128

    val STAND_COLOR_DARK: Color = Color(60, 60, 65)
    val FAN_COLORS: List<Color> =
        listOf(Color(220, 80, 80), Color(80, 120, 220), Color(230, 210, 60), Color(240, 240, 240))
    val STAND_SHADES: List<Color> = listOf(STAND_COLOR_DARK, Color(80, 80, 88), Color(100, 100, 110), Color(120, 120, 130))

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        theme: FieldTheme,
        side: TeamSide,
    ) {
        val crowd = crowdColors(theme, side)
        val random = Random(SEED)
        val fanSize = (FAN_DOT_SIZE * area.scale).toInt().coerceAtLeast(MIN_FAN_SIZE)
        val fanSpacing = (FAN_COL_SPACING * area.scale).toInt().coerceAtLeast(MIN_FAN_SPACING)
        val tierHeight = tierHeight(area)
        repeat(tierCount(area)) { tier ->
            val tierTopY = area.groundY - (tier + 1) * tierHeight
            val tierShade = STAND_SHADES[STAND_SHADES.lastIndex - tier % STAND_SHADES.size]
            g.color = tierShade
            g.fillRect(0, tierTopY, area.width, tierHeight)
            val fanY = tierTopY + tierHeight / 2 - fanSize / 2
            var x = fanSpacing / 2 + (tier % 2) * fanSpacing / 2
            while (x < area.width) {
                g.color = againstTier(crowd[random.nextInt(crowd.size)], tierShade)
                g.fillOval(x, fanY, fanSize, fanSize)
                x += fanSpacing
            }
        }
    }

    fun standsTopY(area: BackdropArea): Int = area.groundY - tierCount(area) * tierHeight(area)

    private fun tierHeight(area: BackdropArea): Int = (TIER_HEIGHT * area.scale).toInt().coerceAtLeast(MIN_TIER_HEIGHT)

    private fun tierCount(area: BackdropArea): Int {
        val tierHeight = tierHeight(area)
        return (area.groundY - area.topY + tierHeight - 1) / tierHeight
    }

    private fun crowdColors(
        theme: FieldTheme,
        side: TeamSide,
    ): List<Color> {
        val endZoneTeam = if (theme.style == FieldStyle.HOME_FIELD) theme.homeTeam else theme.endZoneOf(side).team
        val visitor = if (endZoneTeam === theme.homeTeam) theme.awayTeam else theme.homeTeam
        return List(HOME_PRIMARY_SHARE) { FieldColors.parseColor(endZoneTeam.primaryColor) } +
            List(HOME_SECONDARY_SHARE) { FieldColors.parseColor(endZoneTeam.secondaryColor) } +
            List(HOME_VISITOR_SHARE) { FieldColors.parseColor(visitor.primaryColor) }
    }

    private fun againstTier(
        color: Color,
        tierShade: Color,
    ): Color = if (ColorSimilarity.areSimilar(color, tierShade)) contrastOf(color) else color

    private fun contrastOf(color: Color): Color {
        val lightness = (color.red + color.green + color.blue) / 3
        return if (lightness < TIER_CONTRAST_MIDPOINT) FAN_COLORS.last() else STAND_COLOR_DARK
    }
}
