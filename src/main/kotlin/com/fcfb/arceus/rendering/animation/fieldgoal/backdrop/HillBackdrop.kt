package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D

object HillBackdrop {
    private const val BERM_HEIGHT = 0.42f
    private const val MIN_BERM_HEIGHT = 12

    val PALETTE: List<Color> = GrassPainter.PALETTE

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
    ) {
        GrassPainter.paintStriped(g, area, bermTopY(area))
    }

    fun bermTopY(area: BackdropArea): Int = area.groundY - maxOf((area.height * BERM_HEIGHT).toInt(), area.scaled(MIN_BERM_HEIGHT))
}
