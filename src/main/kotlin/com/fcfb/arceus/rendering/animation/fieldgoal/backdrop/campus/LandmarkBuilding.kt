package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import java.awt.Color
import java.awt.Graphics2D

interface LandmarkBuilding {
    val palette: List<Color>

    fun paint(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        width: Int,
        unit: Float,
    )
}
