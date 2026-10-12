package com.fcfb.arceus.rendering.animation

import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.rendering.animation.appearance.FieldColors
import com.fcfb.arceus.rendering.animation.assets.LogoLoader
import com.fcfb.arceus.rendering.animation.assets.LogoPalette
import com.fcfb.arceus.rendering.animation.fieldgoal.GoalPostScenePainter
import java.awt.Color

object AnimationPalette {
    private const val COLORS_PER_LOGO = 40
    private const val MAX_GIF_COLORS = 256

    fun forTheme(theme: FieldTheme): List<Color> {
        val fixed =
            listOf(
                FieldColors.TURF_COLOR,
                FieldColors.LINE_COLOR,
                FieldColors.BALL_COLOR,
                FieldColors.LOS_COLOR,
                FieldColors.FIRST_DOWN_COLOR,
                Color.BLACK,
                GoalPostScenePainter.SKY_COLOR,
                GoalPostScenePainter.POST_COLOR,
                GoalPostScenePainter.DEFENDER_COLOR,
                GoalPostScenePainter.NET_COLOR,
                GoalPostScenePainter.DEFAULT_WALL_COLOR,
                GoalPostScenePainter.SILVER_WALL_COLOR,
            ) + GoalPostScenePainter.FAN_COLORS + GoalPostScenePainter.STAND_SHADES
        val teams = listOf(theme.homeTeam, theme.awayTeam)
        val teamColors =
            teams.flatMap {
                listOf(FieldColors.parseColor(it.primaryColor), FieldColors.parseColor(it.secondaryColor))
            }
        val logoColors =
            (teams.map { it.scorebugLogo } + theme.centerLogoUrl)
                .distinct()
                .mapNotNull { LogoLoader.load(it) }
                .flatMap { LogoPalette.dominantColors(it, COLORS_PER_LOGO) }
        return (fixed + teamColors + logoColors).distinct().take(MAX_GIF_COLORS)
    }
}
