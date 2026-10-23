package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object AlpineMountains : MountainStyle {
    private const val FOOTHILL_TREE_HEIGHT = 0.11f

    val FAR_RANGE = MountainRange(Color(170, 184, 206), 0.78f, 1.0f, 0.48f, 0.6f, 0.66f, Color(132, 148, 176), Color(196, 208, 226))
    val MID_RANGE = MountainRange(Color(104, 120, 146), 0.52f, 0.74f, 0.3f, 0.42f, 0.58f, Color(74, 88, 114), Color(170, 184, 208))
    val FOOTHILLS = MountainRange(Color(54, 80, 60), 0.2f, 0.3f, 0.12f, 0.18f)

    override val palette: List<Color> =
        listOf(FAR_RANGE, MID_RANGE, FOOTHILLS).flatMap { it.palette } + RidgePainter.SNOW_COLOR + TreePainter.PINE_SHADES

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        RidgePainter.paintRange(g, random, area, FAR_RANGE)
        RidgePainter.paintRange(g, random, area, MID_RANGE)
        val foothills = RidgePainter.paintRange(g, random, area, FOOTHILLS)
        RidgePainter.paintPinesAlong(g, random, area, foothills, FOOTHILL_TREE_HEIGHT)
    }
}
