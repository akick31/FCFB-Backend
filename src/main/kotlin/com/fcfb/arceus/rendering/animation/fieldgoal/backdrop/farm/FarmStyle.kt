package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

interface FarmStyle {
    val palette: List<Color>

    fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    )
}
