package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

interface CampusStyle {
    val palette: List<Color>
    val kindCount: Int

    fun paintBuilding(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
        kind: Int,
    ): Int

    fun paintTree(
        g: Graphics2D,
        random: Random,
        centerX: Int,
        groundY: Int,
        height: Int,
    ) = TreePainter.deciduous(g, random, centerX, groundY, height)
}
