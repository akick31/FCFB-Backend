package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D

interface FacilityStyle {
    val palette: List<Color>
    val fillsView: Boolean

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
    )

    fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount
}
