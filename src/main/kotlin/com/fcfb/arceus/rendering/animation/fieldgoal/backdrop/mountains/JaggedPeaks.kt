package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object JaggedPeaks : MountainStyle {
    private const val FOOTHILL_TREE_HEIGHT = 0.1f

    private val farRange =
        MountainRange(
            Color(164, 178, 202),
            0.86f,
            1.0f,
            0.5f,
            0.62f,
            0.7f,
            Color(122, 138, 168),
            Color(192, 204, 224),
            anchorSegments = 16,
            roughness = 0.62f,
            displacement = 0.22f,
        )
    private val midRange =
        MountainRange(
            Color(96, 110, 136),
            0.62f,
            0.86f,
            0.3f,
            0.42f,
            0.6f,
            Color(66, 78, 104),
            Color(166, 180, 206),
            anchorSegments = 16,
            roughness = 0.62f,
            displacement = 0.22f,
        )
    private val foothills = MountainRange(Color(50, 74, 58), 0.18f, 0.26f, 0.1f, 0.15f)

    override val palette: List<Color> =
        listOf(farRange, midRange, foothills).flatMap { it.palette } + RidgePainter.SNOW_COLOR + TreePainter.PINE_SHADES

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        RidgePainter.paintRange(g, random, area, farRange)
        RidgePainter.paintRange(g, random, area, midRange)
        val ridge = RidgePainter.paintRange(g, random, area, foothills)
        RidgePainter.paintPinesAlong(g, random, area, ridge, FOOTHILL_TREE_HEIGHT)
    }
}
