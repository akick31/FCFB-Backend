package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

object HillShore : LakeShore {
    private const val FAR_HILL_HEIGHT = 0.16f
    private const val FAR_HILL_WAVES = 2.5
    private const val TREELINE_HEIGHT = 0.08f

    private val farHillColor = Color(110, 140, 132)
    private val treelineColor = Color(44, 82, 56)

    override val horizonShare = 0.38f
    override val reflection: Color = Color(46, 98, 112)
    override val palette: List<Color> = listOf(farHillColor, treelineColor, reflection) + TreePainter.PALETTE

    override fun paintFarShore(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
    ) {
        val hillHeight = area.height * FAR_HILL_HEIGHT
        val hill = Path2D.Float()
        hill.moveTo(0.0, horizonY.toDouble())
        for (x in 0..area.width step 8) {
            val wave = (sin(x * FAR_HILL_WAVES * 2 * PI / area.width) + 1) / 2
            hill.lineTo(x.toDouble(), horizonY - hillHeight * (0.45 + 0.55 * wave))
        }
        hill.lineTo(area.width.toDouble(), horizonY.toDouble())
        hill.closePath()
        g.color = farHillColor
        g.fill(hill)
        val treeHeight = (area.height * TREELINE_HEIGHT).toInt().coerceAtLeast(3)
        var x = 0
        while (x < area.width) {
            val height = (treeHeight * (0.7f + random.nextFloat() * 0.5f)).toInt()
            if (random.nextBoolean()) {
                TreePainter.pine(g, random, x, horizonY + 1, height, treelineColor)
            } else {
                TreePainter.deciduous(g, random, x, horizonY + 1, height, treelineColor)
            }
            x += (treeHeight / 2).coerceAtLeast(2)
        }
    }
}
