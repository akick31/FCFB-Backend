package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.AlpineMountains
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.RidgePainter
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object MountainShore : LakeShore {
    private const val TREELINE_HEIGHT = 0.14f

    override val horizonShare = 0.32f
    override val reflection: Color = Color(70, 104, 128)
    override val palette: List<Color> = AlpineMountains.palette + reflection

    override fun paintFarShore(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
    ) {
        val mountainArea = area.copy(groundY = horizonY)
        RidgePainter.paintRange(g, random, mountainArea, AlpineMountains.FAR_RANGE)
        RidgePainter.paintRange(g, random, mountainArea, AlpineMountains.MID_RANGE)
        val shoreline = FloatArray(RidgePainter.SEGMENTS + 1) { 0.02f }
        RidgePainter.paintPinesAlong(g, random, mountainArea, shoreline, TREELINE_HEIGHT)
    }
}
