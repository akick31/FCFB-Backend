package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

interface MountainStyle {
    val palette: List<Color>

    fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    )
}
