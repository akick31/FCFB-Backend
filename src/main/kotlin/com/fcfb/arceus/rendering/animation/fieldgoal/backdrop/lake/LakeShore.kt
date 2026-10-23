package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

interface LakeShore {
    val palette: List<Color>
    val horizonShare: Float
    val reflection: Color?

    fun paintFarShore(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
    )
}
