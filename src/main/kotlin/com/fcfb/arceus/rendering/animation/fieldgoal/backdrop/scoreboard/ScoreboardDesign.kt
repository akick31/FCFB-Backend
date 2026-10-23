package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import com.fcfb.arceus.model.animation.GameSituation
import java.awt.Color
import java.awt.Graphics2D

interface ScoreboardDesign {
    val palette: List<Color>
    val face: Color
    val trim: Color
    val text: Color

    fun paint(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
        homeColor: Color?,
        awayColor: Color?,
    )
}
