package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color

object AnimationPalette {
    private const val COLORS_PER_LOGO = 40
    private const val MAX_GIF_COLORS = 256

    fun forTheme(theme: FieldTheme): List<Color> {
        val fixed =
            listOf(
                FieldBackgroundPainter.TURF_COLOR,
                FieldBackgroundPainter.LINE_COLOR,
                FieldBackgroundPainter.BALL_COLOR,
                FieldBackgroundPainter.LOS_COLOR,
                FieldBackgroundPainter.FIRST_DOWN_COLOR,
                Color.BLACK,
                GoalPostScenePainter.SKY_COLOR,
                GoalPostScenePainter.POST_COLOR,
                GoalPostScenePainter.DEFENDER_COLOR,
                GoalPostScenePainter.NET_COLOR,
            ) + GoalPostScenePainter.FAN_COLORS + GoalPostScenePainter.STAND_SHADES
        val teams = listOf(theme.homeTeam, theme.awayTeam)
        val teamColors =
            teams.flatMap {
                listOf(FieldBackgroundPainter.parseColor(it.primaryColor), FieldBackgroundPainter.parseColor(it.secondaryColor))
            }
        val logoColors =
            (teams.map { it.scorebugLogo } + theme.centerLogoUrl)
                .distinct()
                .mapNotNull { LogoLoader.load(it) }
                .flatMap { LogoPalette.dominantColors(it, COLORS_PER_LOGO) }
        return (fixed + teamColors + logoColors).distinct().take(MAX_GIF_COLORS)
    }
}
