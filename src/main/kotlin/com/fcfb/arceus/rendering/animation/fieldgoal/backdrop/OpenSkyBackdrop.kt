package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D

object OpenSkyBackdrop {
    private const val GRASS_HEIGHT = 20

    val PALETTE: List<Color> = GrassPainter.PALETTE

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
    ) {
        GrassPainter.paint(g, area, area.groundY - area.scaled(GRASS_HEIGHT))
    }
}
